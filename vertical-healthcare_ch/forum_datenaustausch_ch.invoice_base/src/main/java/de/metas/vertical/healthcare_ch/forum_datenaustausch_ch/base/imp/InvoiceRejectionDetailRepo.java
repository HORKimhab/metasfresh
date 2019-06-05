package de.metas.vertical.healthcare_ch.forum_datenaustausch_ch.base.imp;

/*
 * #%L
 * metasfresh-pharma
 * %%
 * Copyright (C) 2018 metas GmbH
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

import com.google.common.base.Joiner;
import de.metas.attachments.AttachmentEntryService;
import de.metas.invoice_gateway.spi.model.imp.ImportedInvoiceResponse;
import lombok.NonNull;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.I_C_Invoice_Rejection_Detail;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRejectionDetailRepo
{
	private static final String REASON_SEPARATOR = "\n";

	private final AttachmentEntryService attachmentEntryService;

	public InvoiceRejectionDetailRepo(@NonNull final AttachmentEntryService attachmentEntryService)
	{
		this.attachmentEntryService = attachmentEntryService;
	}

	private I_C_Invoice_Rejection_Detail of(@NonNull final ImportedInvoiceResponse response)
	{
		final I_C_Invoice_Rejection_Detail rejectionDetail = InterfaceWrapperHelper.newInstance(I_C_Invoice_Rejection_Detail.class);
		rejectionDetail.setIsDone(false);
		rejectionDetail.setInvoiceNumber(response.getDocumentNumber());
		if (response.getInvoiceId() != null)
		{
			rejectionDetail.setC_Invoice_ID(response.getInvoiceId().getRepoId());
		}
		rejectionDetail.setClient(response.getClient());
		rejectionDetail.setInvoiceRecipient(response.getInvoiceRecipient());
		rejectionDetail.setReason(Joiner.on(REASON_SEPARATOR).join(response.getReason()));
		rejectionDetail.setExplanation(response.getExplanation());
		rejectionDetail.setResponsiblePerson(response.getResponsiblePerson());
		rejectionDetail.setPhone(response.getPhone());
		rejectionDetail.setEMail(response.getEmail());
		rejectionDetail.setStatus(response.getStatus().name());
		rejectionDetail.setAD_Org_ID(response.getBillerOrg());

		return rejectionDetail;
	}

	public void save(@NonNull ImportedInvoiceResponse importedInvoiceResponse)
	{
		final I_C_Invoice_Rejection_Detail invoiceRejectionDetail = of(importedInvoiceResponse);
		InterfaceWrapperHelper.saveRecord(invoiceRejectionDetail);
	}

}
