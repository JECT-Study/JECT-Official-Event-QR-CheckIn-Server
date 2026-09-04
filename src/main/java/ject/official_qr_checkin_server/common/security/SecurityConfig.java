package ject.official_qr_checkin_server.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

	private static final RequestMatcher ADMIN_CSRF_MATCHER = new AndRequestMatcher(
		CsrfFilter.DEFAULT_CSRF_MATCHER,
		PathPatternRequestMatcher.pathPattern("/admin/**")
	);

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService adminUserDetailsService(
		@Value("${app.security.admin.username}") String username,
		@Value("${app.security.admin.password}") String password,
		PasswordEncoder passwordEncoder
	) {
		UserDetails admin = User.builder()
			.username(username)
			.password(passwordEncoder.encode(password))
			.roles("ADMIN")
			.build();

		return new InMemoryUserDetailsManager(admin);
	}

	@Bean
	SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		SecurityErrorResponseHandler securityErrorResponseHandler
	) throws Exception {
		http
			.csrf(csrf -> csrf
				.spa()
				.requireCsrfProtectionMatcher(ADMIN_CSRF_MATCHER)
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().permitAll()
			)
			.httpBasic(basic -> basic.authenticationEntryPoint(securityErrorResponseHandler))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(securityErrorResponseHandler)
				.accessDeniedHandler(securityErrorResponseHandler)
			)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
