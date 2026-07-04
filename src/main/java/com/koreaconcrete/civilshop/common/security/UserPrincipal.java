package com.koreaconcrete.civilshop.common.security;

import java.util.List;

public record UserPrincipal(Long id, String email, List<String> roles) {
}
