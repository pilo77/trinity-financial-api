package com.trinity.financial.account.entity;

public enum AccountType {
    SAVINGS("53"),
    CHECKING("33");

    private final String prefix;

    AccountType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
