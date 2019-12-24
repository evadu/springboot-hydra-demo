package org.ddl.demo.hydra;

import com.github.ory.hydra.ApiClient;
import com.github.ory.hydra.api.AdminApi;
import com.github.ory.hydra.api.PublicApi;
import com.github.ory.hydra.model.*;
import org.apache.logging.log4j.util.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SpringBootApplication
@RestController
public class HydraDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(HydraDemoApplication.class).start();
    }


    private String basePath = "http://127.0.0.1";

    private String redirectUri = "https://192.168.45.81/callback";

    private String clientId = "java-client";
    private String clientSecret = "mySecret";

    private String tokenKey = "hydraToken";

    private ApiClient getApi(String port) {
        ApiClient apiClient = new ApiClient();
        apiClient.setUsername(clientId);
        apiClient.setPassword(clientSecret);
        apiClient.setBasePath(basePath + ":" + port);
        return apiClient;
    }

    @GetMapping("/")
    public String index(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object tokenInfo = request.getSession().getAttribute(tokenKey);
        if (Objects.isNull(tokenInfo)) {
            String url = "http://127.0.0.1:4444/oauth2/auth?" +
                    "client_id=" + clientId +
                    "&response_type=code&scope=openid%20offline" +
                    "&state=" + UUID.randomUUID().toString().substring(0, 8)
                    + "&audience=" +
                    "&redirect_uri=" + redirectUri;
            response.sendRedirect(url);
        } else {
            Oauth2TokenResponse data = (Oauth2TokenResponse) tokenInfo;
            if (data.getExpiresIn() < System.currentTimeMillis()) {
                //利用token去刷新token
                data = new PublicApi(getApi("4444")).oauth2Token("refresh_token", null, data.getRefreshToken(), redirectUri, clientId);

            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AccessToken:");
            stringBuilder.append(data.getAccessToken());
            return stringBuilder.toString();
        }
        return Strings.EMPTY;
    }

    @GetMapping("/callback")
    public void callback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!request.getParameterMap().isEmpty()) {
            String[] codeObj = request.getParameterMap().get("code");
            if (!Objects.isNull(codeObj)) {
                String code = codeObj[0];
                PublicApi publicApi = new PublicApi(getApi("4444"));
                Oauth2TokenResponse data = publicApi.oauth2Token("authorization_code", code, null, redirectUri, clientId);
                data.setExpiresIn(data.getExpiresIn() * 1000 + System.currentTimeMillis() - 5);
                request.getSession().setAttribute(tokenKey, data);
                response.sendRedirect("/");
            }
        }
    }

    @GetMapping("/login")
    public String loginUi(String login_challenge) {
        return "default Login UI. username:admin & pwd:123 <br/><a href=\"/login/admin/123/" + login_challenge + "\">submit</a>";
    }

    @GetMapping("/login/{username}/{pwd}/{login_challenge}")
    public void login(@PathVariable("username") String username, @PathVariable("pwd") String pwd, @PathVariable("login_challenge") String challenges, HttpServletResponse response) throws IOException {
        Map params = new HashMap();
        if (username.equals("admin") && pwd.equals("123")) {
            params.put("username", username);
            params.put("userid", pwd);
            AcceptLoginRequest body = new AcceptLoginRequest();
            body.setSubject("admin");
            body.setContext(params);
            AdminApi adminApi = new AdminApi(getApi("4445"));
            CompletedRequest data = adminApi.acceptLoginRequest(challenges, body);
            response.sendRedirect(data.getRedirectTo());
        } else {
            response.sendRedirect("/login?login_challenge=" + challenges);
        }
    }

    @GetMapping("/consent")
    public String consentUi(String consent_challenge) {
        return " This is Auth UI .default  Agree <a href=\"/consent/" + consent_challenge + "\">Agree</a>";
    }

    @GetMapping("/consent/{consent_challenge}")
    public void consent(@PathVariable("consent_challenge") String challenges, HttpServletResponse response) throws IOException {

        AdminApi adminApi = new AdminApi(getApi("4445"));
        ConsentRequest result = adminApi.getConsentRequest(challenges);
        AcceptConsentRequest body = new AcceptConsentRequest();
        body.setGrantAccessTokenAudience(result.getRequestedAccessTokenAudience());
        CompletedRequest data = adminApi.acceptConsentRequest(challenges, body);
        response.sendRedirect(data.getRedirectTo());
    }
}
