package de.yusufakcay.contractmanagement.shared.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("springSecurityAuditorAware")
public final class SpringSecurityAuditorAware implements AuditorAware<String> {

	@Override
	@NonNull
	public Optional<String> getCurrentAuditor() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
			.filter(JwtAuthenticationToken.class::isInstance)
			.map(JwtAuthenticationToken.class::cast)
			.map(JwtAuthenticationToken::getToken)
			.map(jwt -> Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(jwt.getSubject()));
	}
}
