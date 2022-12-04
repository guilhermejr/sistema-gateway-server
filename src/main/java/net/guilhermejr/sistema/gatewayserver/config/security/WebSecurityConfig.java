package net.guilhermejr.sistema.gatewayserver.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Value("${sistema.eureka.login}")
    private String eurekaLogin;

    @Value("${sistema.eureka.senha}")
    private String eurekaSenha;

    @Value("${spring.boot.admin.client.instance.metadata.user.name}")
    private String actuatorLogin;

    @Value("${spring.boot.admin.client.instance.metadata.user.password}")
    private String actuatorSenha;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic()
                .and()
                .authorizeRequests()
                .antMatchers("/actuator/**").hasRole("ACTUATOR")
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
                .formLogin();
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails eureka = User.builder()
                .username(eurekaLogin).password(passwordEncoder().encode(eurekaSenha)).roles("ADMIN").build();
        UserDetails actuator = User.builder()
                .username(actuatorLogin).password(passwordEncoder().encode(actuatorSenha)).roles("ACTUATOR").build();
        return new InMemoryUserDetailsManager(eureka, actuator);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

