package com.example.jenkinsdemo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JenkinsService {

    @Value("${jenkins.url}")
    private String jenkinsUrl;

    @Value("${jenkins.username}")
    private String username;

    @Value("${jenkins.api-token}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== 1. 触发构建 ====================

    /**
     * 触发 Jenkins 参数化构建
     * @param jobName 任务名称（如 first-pipeline）
     * @param branch  代码分支
     * @param env     部署环境
     * @return 构建编号（如 "10"），失败返回 null
     */
    public String triggerBuild(String jobName, String branch, String env) {
        try {
            String url = UriComponentsBuilder.fromUriString(jenkinsUrl)
                    .pathSegment("job", jobName, "buildWithParameters")
                    .queryParam("BRANCH", branch)
                    .queryParam("ENV", env)
                    .build()
                    .toUriString();

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 触发成功 Jenkins 返回 201 Created 或 302 Found
            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection()) {
                String location = response.getHeaders().getFirst("Location");
                if (location != null) {
                    // 从 Location 中提取构建编号，如 /job/first-pipeline/10/
                    Pattern pattern = Pattern.compile("/(\\d+)/?$");
                    Matcher matcher = pattern.matcher(location);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
                return null; // 触发成功但无法解析编号
            }
            return null;
        } catch (Exception e) {
            // 实际开发中建议使用 log.error 记录日志
            System.err.println("触发构建失败：" + e.getMessage());
            return null;
        }
    }

    // ==================== 2. 查询构建状态 ====================

    /**
     * 查询指定构建编号的详细状态（返回原始 JSON 字符串）
     * @param jobName     任务名称
     * @param buildNumber 构建编号
     * @return Jenkins 返回的 JSON 字符串，失败返回 null
     */
    public String getBuildStatus(String jobName, int buildNumber) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/api/json";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("查询构建状态失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 查询最后一次构建的状态（简化版）
     * @param jobName 任务名称
     * @return Jenkins 返回的 JSON 字符串
     */
    public String getLastBuildStatus(String jobName) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/lastBuild/api/json";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            System.err.println("查询最后一次构建状态失败：" + e.getMessage());
            return null;
        }
    }

    // ==================== 3. 获取构建日志 ====================

    /**
     * 获取指定构建编号的控制台日志（纯文本）
     * @param jobName     任务名称
     * @param buildNumber 构建编号
     * @return 日志文本（含 ANSI 颜色码），失败返回 null
     */
    public String getBuildLog(String jobName, int buildNumber) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/logText/progressiveText";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 注意：Jenkins 返回的是 text/plain，不是 JSON
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            System.err.println("获取构建日志失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 获取构建日志（带起始字节偏移量，用于流式输出）
     * @param jobName     任务名称
     * @param buildNumber 构建编号
     * @param start       起始字节偏移量
     * @return 日志文本和新的偏移量（用 Map 返回，或使用自定义类）
     */
    public ResponseEntity<String> getBuildLogWithOffset(String jobName, int buildNumber, long start) {
        String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/logText/progressiveText?start=" + start;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    // ==================== 4. （可选）停止构建 ====================

    /**
     * 停止正在运行的构建
     * @param jobName     任务名称
     * @param buildNumber 构建编号
     * @return 是否停止成功
     */
    public boolean stopBuild(String jobName, int buildNumber) {
        try {
            String url = jenkinsUrl + "/job/" + jobName + "/" + buildNumber + "/stop";

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();
        } catch (Exception e) {
            System.err.println("停止构建失败：" + e.getMessage());
            return false;
        }
    }

    // ==================== 私有辅助方法 ====================

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = username + ":" + apiToken;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.ISO_8859_1));
        headers.set("Authorization", "Basic " + new String(encodedAuth));
        return headers;
    }
}