package com.switchwon.devbehomework.exchangerate.provider;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;

public interface ExchangeRateProvider {

	ProviderRate fetchRate(ForeignCurrency from, CurrencyCode to);

	boolean supports(ForeignCurrency from, CurrencyCode to);

	String getName();
}
