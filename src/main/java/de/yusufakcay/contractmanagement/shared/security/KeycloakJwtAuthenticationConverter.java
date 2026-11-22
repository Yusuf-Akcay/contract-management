package de.yusufakcay.contractmanagement.shared.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public final class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
		var authorities = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
			.map(realmAccess -> realmAccess.get("roles"))
			.filter(Collection.class::isInstance)
			.map(roles -> ((Collection<?>) roles).stream()
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
				.toList())
			.orElseGet(List::of);
		var name = Optional.ofNullable(jwt.getClaimAsString("preferred_username")).orElse(jwt.getSubject());
		return new JwtAuthenticationToken(jwt, authorities, name);
	}
}
