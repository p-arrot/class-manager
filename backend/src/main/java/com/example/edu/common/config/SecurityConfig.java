package com.example.edu.common.config;

import com.example.edu.common.result.ErrorCode;
import com.example.edu.common.result.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                                        ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/classes/list-all").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/classes/**").hasRole("ADMIN")
                        .requestMatchers("/api/teachers/**").hasRole("ADMIN")
                        .requestMatchers("/api/files/upload/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/files/*/download").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/files/*/preview").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/files/*/stream").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/files/*/raw").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/lessons/*/tasks", "/api/lessons/*/tasks/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/tasks/*/submit").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/*/submissions").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/tasks/*").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "TEACHER")
                        // Students
                        .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "TEACHER")
                        // Courses, Semesters, Lessons (non-task), Resources
                        .requestMatchers(HttpMethod.GET, "/api/courses/**", "/api/semesters/**", "/api/lessons/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/courses/**", "/api/semesters/**", "/api/lessons/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.GET, "/api/resources/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/resources/**").hasAnyRole("ADMIN", "TEACHER")
                        // Evaluations
                        .requestMatchers(HttpMethod.GET, "/api/students/*/evaluations", "/api/students/*/radar", "/api/evaluations/grade-scores").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/submissions/*/evaluate", "/api/tasks/*/auto-grade").hasAnyRole("ADMIN", "TEACHER")
                        // Exams
                        .requestMatchers(HttpMethod.GET, "/api/exam-papers", "/api/semesters/*/exams", "/api/exams/*/submissions").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/exam-papers/**", "/api/exams/**", "/api/exam-submissions/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/exams/*/start", "/api/exams/*/submit").hasRole("STUDENT")
                        // Projects & Teams
                        .requestMatchers(HttpMethod.GET, "/api/semesters/*/projects").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        .requestMatchers("/api/projects/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/projects/*/teams", "/api/teams/*/join", "/api/projects/*/submit").hasRole("STUDENT")
                        // Drive
                        .requestMatchers("/api/drive/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        // Stats
                        .requestMatchers("/api/stats/**").hasAnyRole("ADMIN", "TEACHER")
                        // Submissions detail
                        .requestMatchers(HttpMethod.GET, "/api/submissions/**").hasAnyRole("ADMIN", "TEACHER", "STUDENT")
                        // WebSocket + OPTIONS
                        .requestMatchers("/ws/**", "/ws").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, int httpStatus, ErrorCode errorCode) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(JSON_ERROR_BYTES.get(errorCode));
    }

    private static final java.util.Map<ErrorCode, byte[]> JSON_ERROR_BYTES =
            java.util.Map.of(
                    ErrorCode.UNAUTHORIZED,
                    ("{\"code\":" + ErrorCode.UNAUTHORIZED.getCode() + ",\"msg\":\"" + ErrorCode.UNAUTHORIZED.getMsg() + "\"}").getBytes(StandardCharsets.UTF_8),
                    ErrorCode.FORBIDDEN,
                    ("{\"code\":" + ErrorCode.FORBIDDEN.getCode() + ",\"msg\":\"" + ErrorCode.FORBIDDEN.getMsg() + "\"}").getBytes(StandardCharsets.UTF_8)
            );

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
