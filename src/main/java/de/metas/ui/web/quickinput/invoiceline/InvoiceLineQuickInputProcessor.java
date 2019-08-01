package de.metas.ui.web.quickinput.invoiceline;

import static de.metas.util.Check.fail;

import org.adempiere.invoice.service.IInvoiceBL;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_C_Invoice;

import de.metas.adempiere.model.I_C_InvoiceLine;
import de.metas.bpartner.BPartnerId;
import de.metas.bpartner_product.IBPartnerProductBL;
import de.metas.invoice.IInvoiceLineBL;
import de.metas.product.ProductId;
import de.metas.ui.web.quickinput.IQuickInputProcessor;
import de.metas.ui.web.quickinput.QuickInput;
import de.metas.ui.web.window.datatypes.DocumentId;
import de.metas.util.Services;

/*
 * #%L
 * metasfresh-webui-api
 * %%
 * Copyright (C) 2017 metas GmbH
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

public class InvoiceLineQuickInputProcessor implements IQuickInputProcessor
{

	@Override
	public DocumentId process(final QuickInput quickInput)
	{
		final IInvoiceBL invoiceBL = Services.get(IInvoiceBL.class);
		final IInvoiceLineBL invoiceLineBL = Services.get(IInvoiceLineBL.class);

		final I_C_Invoice invoice = quickInput.getRootDocumentAs(I_C_Invoice.class);
		final IInvoiceLineQuickInput invoiceLineQuickInput = quickInput.getQuickInputDocumentAs(IInvoiceLineQuickInput.class);

		// 3834
		final ProductId productId = ProductId.ofRepoId(invoiceLineQuickInput.getM_Product_ID());
		final BPartnerId partnerId = BPartnerId.ofRepoId(invoice.getC_BPartner_ID());
		Services.get(IBPartnerProductBL.class).assertNotExcludedFromSaleToCustomer(productId, partnerId);

		final I_C_InvoiceLine invoiceLine = InterfaceWrapperHelper.newInstance(I_C_InvoiceLine.class, invoice);
		invoiceLine.setC_Invoice(invoice);

		invoiceBL.setProductAndUOM(invoiceLine, invoiceLineQuickInput.getM_Product_ID());
		fail("NOT YET IMPLEMENTED"); // TODO https://github.com/metasfresh/metasfresh/issues/5384
		//invoiceBL.setQtys(invoiceLine, invoiceLineQuickInput.getQty());

		invoiceLineBL.updatePrices(invoiceLine);
		// invoiceBL.setLineNetAmt(invoiceLine); // not needed; will be called on save
		// invoiceBL.setTaxAmt(invoiceLine);// not needed; will be called on save

		InterfaceWrapperHelper.save(invoiceLine);

		return DocumentId.of(invoiceLine.getC_InvoiceLine_ID());
	}

}
