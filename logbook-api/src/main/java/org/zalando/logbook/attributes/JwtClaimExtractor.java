package org.zalando.logbook.attributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.logbook.HttpRequest;

import javax.annotation.Nonnull;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@AllArgsConstructor
public final class JwtClaimExtractor implements RequestAttributesExtractor {

    private static final String BEARER_JWT_PATTERN = "Bearer [a-z0-9-_]+\\.([a-z0-9-_]+)\\.[a-z0-9-_]+";
    private static final Pattern pattern = Pattern.compile(BEARER_JWT_PATTERN, Pattern.CASE_INSENSITIVE);

    // RFC 7519 section-4.1.2: The "sub" (subject) claim identifies the principal that is the subject of the JWT.
    public static final String DEFAULT_SUBJECT_CLAIM = "sub";

    public static final String DEFAULT_CLAIM_KEY = "subject";

    @Nonnull
    private final ObjectMapper objectMapper;

    @Nonnull
    private final List<String> claimNames;

    @Nonnull
    private final String claimKey;

    public JwtClaimExtractor() {
        this(new ObjectMapper(), Collections.singletonList(DEFAULT_SUBJECT_CLAIM), DEFAULT_CLAIM_KEY);
    }

    @Nonnull
    @Override
    public HttpAttributes extract(HttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null) return HttpAttributes.EMPTY;

        Matcher matcher = pattern.matcher(authHeader);
        if (!matcher.matches()) return HttpAttributes.EMPTY;

        try {
            String payload = new String(Base64.getUrlDecoder().decode(matcher.group(1)));
            HashMap<?, ?> claims = objectMapper.readValue(payload, HashMap.class);
            return claimNames.stream()
                    .map(claims::get)
                    .filter(value -> value instanceof String)
                    .findFirst()
                    .map(value -> HttpAttributes.of(claimKey, value))
                    .orElse(HttpAttributes.EMPTY);
        } catch (Exception e) {
            return HttpAttributes.EMPTY;
        }
    }

}
