package com.acme.salary.reference;

/** A country where employees are located; its currency is the currency of their salaries. */
public record Country(String code, String name, String currencyCode) {
}
