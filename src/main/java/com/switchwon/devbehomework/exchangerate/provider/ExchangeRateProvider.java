package com.switchwon.devbehomework.exchangerate.provider;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;

public interface ExchangeRateProvider {

	ProviderRate fetchRate(RatedCurrency from, Currency to);

	boolean supports(RatedCurrency from, Currency to);

	String getName();
}
