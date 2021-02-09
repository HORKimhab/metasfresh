package de.metas.contracts.subscription.callout;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.ad.callout.annotations.Callout;
import org.adempiere.ad.callout.annotations.CalloutMethod;
import org.adempiere.ad.callout.api.ICalloutField;
import org.adempiere.ad.trx.api.ITrx;
import org.compiere.model.I_C_Order;
import org.compiere.util.Env;

import de.metas.bpartner.BPartnerLocationId;
import de.metas.contracts.model.I_C_Flatrate_Conditions;
import de.metas.contracts.model.I_C_Flatrate_Matching;
import de.metas.contracts.order.model.I_C_OrderLine;
import de.metas.contracts.subscription.ISubscriptionBL;
import de.metas.lang.SOTrx;
import de.metas.order.IOrderLineBL;
import de.metas.order.OrderLinePriceUpdateRequest;
import de.metas.order.OrderLinePriceUpdateRequest.ResultUOM;
import de.metas.pricing.PriceListId;
import de.metas.pricing.PricingSystemId;
import de.metas.pricing.service.IPriceListDAO;
import de.metas.product.IProductDAO;
import de.metas.product.ProductAndCategoryId;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;
import de.metas.uom.IUOMConversionBL;
import de.metas.util.Services;
import lombok.NonNull;

@Callout(I_C_OrderLine.class)
public class C_OrderLine
{
	private final transient IOrderLineBL orderLineBL = Services.get(IOrderLineBL.class);

	@CalloutMethod(columnNames = { I_C_OrderLine.COLUMNNAME_C_Flatrate_Conditions_ID })
	public void onFlatrateConditions(final I_C_OrderLine ol, final ICalloutField field)
	{
		final ProductId productId = ProductId.ofRepoIdOrNull(ol.getM_Product_ID());
		final int bPartnerId = ol.getC_BPartner_ID();

		final I_C_Order order = ol.getC_Order();
		final SOTrx soTrx = SOTrx.ofBoolean(order.isSOTrx());

		if (productId == null || bPartnerId <= 0 || soTrx.isPurchase())
		{
			return;
		}

		final boolean updatePriceEnteredAndDiscountOnlyIfNotAlreadySet = false; // when the subscription changed, update all prices

		final int subscriptionId = ol.getC_Flatrate_Conditions_ID();
		if (subscriptionId <= 0)
		{
			final BigDecimal qtyOrdered = orderLineBL.convertQtyEnteredToStockUOM(ol).toBigDecimal();
			ol.setQtyOrdered(qtyOrdered);
			ol.setQtyEnteredInPriceUOM(qtyOrdered);

			orderLineBL.updatePrices(OrderLinePriceUpdateRequest.builder()
					.orderLine(ol)
					.resultUOM(ResultUOM.PRICE_UOM)
					.updatePriceEnteredAndDiscountOnlyIfNotAlreadySet(updatePriceEnteredAndDiscountOnlyIfNotAlreadySet)
					.updateLineNetAmt(true)
					.build());

			return;
		}

		updatePrices(ol, soTrx, updatePriceEnteredAndDiscountOnlyIfNotAlreadySet);
	}

	@CalloutMethod(columnNames = { I_C_OrderLine.COLUMNNAME_QtyEntered })
	public void onQtyEntered(final I_C_OrderLine ol, final ICalloutField field)
	{

		final I_C_Order order = ol.getC_Order();
		final SOTrx soTrx = SOTrx.ofBoolean(order.isSOTrx());

		if (soTrx.isPurchase() || ol.getC_Flatrate_Conditions_ID() <= 0)
		{
			return; // leave this job to the adempiere standard callouts
		}

		final boolean updatePriceEnteredAndDiscountOnlyIfNotAlreadySet = true;
		updatePrices(ol, soTrx, updatePriceEnteredAndDiscountOnlyIfNotAlreadySet);
	}

	private void updatePrices(
			@NonNull final I_C_OrderLine ol,
			@NonNull final SOTrx soTrx,
			final boolean updatePriceEnteredAndDiscountOnlyIfNotAlreadySet)
	{
		final ISubscriptionBL subscriptionBL = Services.get(ISubscriptionBL.class);
		final IUOMConversionBL uomConversionBL = Services.get(IUOMConversionBL.class);
		final IOrderLineBL orderLineBL = Services.get(IOrderLineBL.class);
		final IPriceListDAO priceListDAO = Services.get(IPriceListDAO.class);

		final I_C_Flatrate_Conditions flatrateConditions = ol.getC_Flatrate_Conditions();
		final I_C_Order order = ol.getC_Order();

		final PricingSystemId pricingSysytemId;

		if (flatrateConditions.getM_PricingSystem_ID() > 0)
		{
			pricingSysytemId = PricingSystemId.ofRepoId(flatrateConditions.getM_PricingSystem_ID());
		}
		else
		{
			pricingSysytemId = PricingSystemId.ofRepoIdOrNull(order.getM_PricingSystem_ID());
		}

		final BPartnerLocationId bpLocationId = BPartnerLocationId.ofRepoId(ol.getC_BPartner_ID(), ol.getC_BPartner_Location_ID());
		final Timestamp date = order.getDateOrdered();

		final PriceListId subscriptionPLId = priceListDAO.retrievePriceListIdByPricingSyst(pricingSysytemId, bpLocationId, soTrx);

		final int numberOfRuns = subscriptionBL.computeNumberOfRuns(flatrateConditions.getC_Flatrate_Transition(), date);

		final Properties ctx = Env.getCtx();
		final ProductId productId = ProductId.ofRepoIdOrNull(ol.getM_Product_ID());
		final ProductAndCategoryId productAndCategoryId = Services.get(IProductDAO.class).retrieveProductAndCategoryIdByProductId(productId);
		final I_C_Flatrate_Matching matching = subscriptionBL.retrieveMatching(
				ctx,
				ol.getC_Flatrate_Conditions_ID(),
				productAndCategoryId,
				ITrx.TRXNAME_None);

		final Quantity qtyEntered = orderLineBL.getQtyEntered(ol);
		final Quantity qtyOrdered = uomConversionBL.convertToProductUOM(qtyEntered, productId);

		final Quantity qtyOrderedPerRun;
		if (matching != null && matching.getQtyPerDelivery().signum() > 0)
		{
			final Quantity qtyPerDelivery = Quantity.of(matching.getQtyPerDelivery(), qtyOrdered.getUOM());
			qtyOrderedPerRun = qtyPerDelivery.min(qtyOrdered);
		}
		else
		{
			qtyOrderedPerRun = qtyOrdered;
		}

		// priceQty is the qty do be delivered during one complete subscription term
		final Quantity priceQty = qtyOrderedPerRun.multiply(numberOfRuns);

		// qty ordered needs to be set because it will be used to compute the
		// line's NetLineAmount in MOrderLine.beforeSave()
		ol.setQtyOrdered(priceQty.toBigDecimal());

		ol.setQtyEnteredInPriceUOM(priceQty.toBigDecimal());

		// now compute the new prices
		orderLineBL.updatePrices(OrderLinePriceUpdateRequest.builder()
				.orderLine(ol)
				.priceListIdOverride(subscriptionPLId)
				.qtyOverride(priceQty)
				.resultUOM(ResultUOM.PRICE_UOM)
				.updatePriceEnteredAndDiscountOnlyIfNotAlreadySet(updatePriceEnteredAndDiscountOnlyIfNotAlreadySet)
				.updateLineNetAmt(true)
				.build());
	}
}
