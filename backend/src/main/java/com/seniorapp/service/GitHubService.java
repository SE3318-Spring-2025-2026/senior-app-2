package com.seniorapp.service;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.GitHubUserResponse;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.StudentWhitelistRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final UserRepository userRepository;
    private final StudentWhitelistRepository whitelistRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubService(UserRepository userRepository, 
                         StudentWhitelistRepository whitelistRepository, 
                         JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.whitelistRepository = whitelistRepository;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse processGitHubLogin(String githubAccessToken, String studentId) {
        // 1. GitHub'dan kullanıcı bilgilerini al
        String url = "https://api.github.com/user";
        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(githubAccessToken);
        var entity = new org.springframework.http.HttpEntity<>(headers);
        
        GitHubUserResponse ghUser = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, GitHubUserResponse.class).getBody();

        if (ghUser == null) {
            log.warn("GitHub /user returned null (legacy flow)");
            throw new RuntimeException("GitHub authentication failed");
        }

        // 2. Whitelist Kontrolü (Berat'ın istediği kritik nokta)
        if (!whitelistRepository.existsByStudentId(studentId)) {
            log.warn("Legacy GitHub login blocked: studentId not on whitelist");
            throw new RuntimeException("Access Denied: Student ID " + studentId + " is not whitelisted by coordinator.");
        }

        // 3. Kullanıcıyı bul veya oluştur
        User user = userRepository.findByGithubId(ghUser.getId())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setGithubId(ghUser.getId());
                    newUser.setGithubUsername(ghUser.getLogin());
                    newUser.setEmail(ghUser.getEmail());
                    newUser.setRole(Role.STUDENT);
                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        log.info("Legacy GitHub login succeeded userId={}", user.getId());
        return new AuthResponse(token, new AuthResponse.UserInfo(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), user.getGithubUsername()
        ));
    }
}