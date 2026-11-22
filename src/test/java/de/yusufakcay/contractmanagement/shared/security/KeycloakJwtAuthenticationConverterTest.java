package de.yusufakcay.contractmanagement.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

	private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

	@Test
	void mapsRealmRolesToRoleAuthorities() {
		Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("contract-read", "contract-write"))));

		var token = converter.convert(jwt);

		assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
			.containsExactlyInAnyOrder("ROLE_contract-read", "ROLE_contract-write");
		assertThat(token.getName()).isEqualTo("clerk");
	}

	@Test
	void noRealmAccessMeansNoRoles() {
		assertThat(converter.convert(jwt(Map.of())).getAuthorities()).isEmpty();
	}

	@Test
	void fallsBackToSubjectWithoutUsername() {
		Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("service-account-4711").build();

		assertThat(converter.convert(jwt).getName()).isEqualTo("service-account-4711");
	}

	private static Jwt jwt(Map<String, Object> claims) {
		return Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.subject("4711")
			.claim("preferred_username", "clerk")
			.claims(c -> c.putAll(claims))
			.build();
	}
}
