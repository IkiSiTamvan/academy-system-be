package com.sitamvan.auth.dto.statval;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationConstant {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ContextPath {

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Auth {
            public static final String USER = "/v1/user";
            public static final String ROLE = "/v1/role";
            public static final String USER_ROLE = "/v1/user-role";
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Path {
        public static final String PAGINATION = "/{page}/{size}";
        public static final String FIND_BY_ID = "/{id}";
    }

}