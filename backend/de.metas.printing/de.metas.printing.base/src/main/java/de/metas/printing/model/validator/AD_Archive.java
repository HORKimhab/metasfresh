package de.metas.printing.model.validator;

/*
 * #%L
 * de.metas.printing.base
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import de.metas.logging.LogManager;
import de.metas.printing.PrintOutputFacade;
import de.metas.printing.api.IPrintingQueueBL;
import de.metas.printing.model.I_AD_Archive;
import de.metas.printing.model.I_C_Doc_Outbound_Config;
import de.metas.util.Services;
import lombok.NonNull;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.model.InterfaceWrapperHelper;
import org.compiere.model.ModelValidator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

@Interceptor(I_AD_Archive.class)
@Component
public class AD_Archive
{
	private final PrintOutputFacade printOutputFacade;
	private final IPrintingQueueBL printingQueueBL = Services.get(IPrintingQueueBL.class);

	public AD_Archive(@NonNull final PrintOutputFacade printOutputFacade)
	{
		this.printOutputFacade = printOutputFacade;
	}

	/**
	 * Check if the archive references a docOutBoundConfig, and if yes, copy its settings (possibly overriding previous settings).
	 * <p>
	 * Note: if the config id is changed to <code>null</code>, then do nothing.
	 * <p>
	 * task http://dewiki908/mediawiki/index.php/09417_Massendruck_-_Sofort-Druckjob_via_Ausgehende-Belege_konfig_einstellbar_%28101934367465%29
	 */
	@ModelChange(timings = { ModelValidator.TYPE_BEFORE_NEW,
			ModelValidator.TYPE_BEFORE_CHANGE },
			ifColumnsChanged = I_AD_Archive.COLUMNNAME_C_Doc_Outbound_Config_ID)
	public void updateArchiveFlags(final I_AD_Archive archive)
	{
		if (archive.getC_Doc_Outbound_Config_ID() <= 0)
		{
			return; // nothing to do
		}

		// task 09417: also check if the archive references a docOutBoundConfig, and if yes, use its settings.
		final I_C_Doc_Outbound_Config config = InterfaceWrapperHelper.create(archive.getC_Doc_Outbound_Config(),
																			 I_C_Doc_Outbound_Config.class);
		archive.setIsDirectEnqueue(config.isDirectEnqueue());
		archive.setIsDirectProcessQueueItem(config.isDirectProcessQueueItem());
	}

	/**
	 * If direct print is required for given {@link AD_Archive} then this method enqueues the archive to printing queue.
	 */
	@ModelChange(timings = { ModelValidator.TYPE_AFTER_NEW, ModelValidator.TYPE_AFTER_CHANGE },
			ifColumnsChanged = {
					I_AD_Archive.COLUMNNAME_IsDirectProcessQueueItem,
					I_AD_Archive.COLUMNNAME_IsDirectEnqueue,
					I_AD_Archive.COLUMNNAME_C_Doc_Outbound_Config_ID,
					I_AD_Archive.COLUMNNAME_IsActive })
	public void printArchive(final I_AD_Archive archive)
	{
		printingQueueBL.printArchive(archive, printOutputFacade, null);
	}
}
