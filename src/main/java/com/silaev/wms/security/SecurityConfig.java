
package com.silaev.wms.security;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    public static final String READ_PRIVILEGE = "READ_PRIVILEGE";
    public static final String WRITE_PRIVILEGE = "WRITE_PRIVILEGE";
	public static final String ADMIN_NAME = "admin";
	public static final String ADMIN_PAS = "admin";
	public static final String ADMIN_ROLE = "ADMIN";
	public static final String USER_NAME = "user";
	public static final String USER_PAS = "user";
	public static final String USER_ROLE = "USER";

	@Bean
	SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange()
                .pathMatchers(HttpMethod.GET, "/**").hasAuthority(READ_PRIVILEGE)
                .pathMatchers(HttpMethod.OPTIONS, "/**").hasAuthority(READ_PRIVILEGE)
                .pathMatchers(HttpMethod.POST, "/**").hasAuthority(WRITE_PRIVILEGE)
                .pathMatchers(HttpMethod.PATCH, "/**").hasAuthority(WRITE_PRIVILEGE)
                .pathMatchers(HttpMethod.PUT, "/**").hasAuthority(WRITE_PRIVILEGE)
                .anyExchange().authenticated()
                .and()
                .httpBasic()
                .and()
                .csrf().disable()
                .build();

	}

	@Bean
	public MapReactiveUserDetailsService userDetailsService() {
		UserDetails user = User
				.withUsername(USER_NAME)
				.password(passwordEncoder().encode(USER_PAS))
				.roles(USER_ROLE)
                .authorities(READ_PRIVILEGE)
				.build();

		UserDetails admin = User
				.withUsername(ADMIN_NAME)
				.password(passwordEncoder().encode(ADMIN_PAS))
				.roles(ADMIN_ROLE)
                .authorities(READ_PRIVILEGE, WRITE_PRIVILEGE)
				.build();

		return new MapReactiveUserDetailsService(user, admin);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new SCryptPasswordEncoder();
	}
}
