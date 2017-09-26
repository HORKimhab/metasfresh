package de.metas.contracts.subscription.impl.subscriptioncommands;

import static org.adempiere.model.InterfaceWrapperHelper.newInstance;
import static org.adempiere.model.InterfaceWrapperHelper.save;

import java.sql.Timestamp;
import java.util.List;

import org.adempiere.model.InterfaceWrapperHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.jgoodies.common.base.Objects;

import de.metas.contracts.subscription.impl.SubscriptionCommand;
import de.metas.flatrate.model.I_C_Flatrate_Term;
import de.metas.flatrate.model.I_C_SubscriptionProgress;
import de.metas.flatrate.model.X_C_SubscriptionProgress;
import lombok.NonNull;

/*
 * #%L
 * de.metas.contracts
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class InsertPause
{
	private final SubscriptionCommand subscriptionCommand;

	public InsertPause(@NonNull final SubscriptionCommand subscriptionCommand)
	{
		this.subscriptionCommand = subscriptionCommand;
	}
	
	public void insertPause(
			@NonNull final I_C_Flatrate_Term term,
			@NonNull final Timestamp pauseFrom,
			@NonNull final Timestamp pauseUntil)
	{
		subscriptionCommand.removePauses(term, pauseFrom, pauseUntil);

		final List<I_C_SubscriptionProgress> allSpsAfterBeginOfPause = subscriptionCommand.retrieveNextSPsAndLogIfEmpty(term, pauseFrom);
		if (allSpsAfterBeginOfPause.isEmpty())
		{
			return;
		}

		final I_C_SubscriptionProgress firstSpAfterBeginOfPause = allSpsAfterBeginOfPause.get(0);
		createBeginOfPause(term, pauseFrom, firstSpAfterBeginOfPause.getSeqNo());

		final ImmutableList<I_C_SubscriptionProgress> updatedSpsWithinPause = updateAndCollectRecordWithinPause(allSpsAfterBeginOfPause, pauseUntil);

		final int endOfPauseSeqNo = computeEndOfPauseSeqNo(firstSpAfterBeginOfPause, updatedSpsWithinPause);

		createEndOfPause(term, pauseUntil, endOfPauseSeqNo);

		final ImmutableList<I_C_SubscriptionProgress> spsAfterEndOfPause = collectSpsAfterEndOfPause(allSpsAfterBeginOfPause, pauseUntil);

		increaseSeqNosByTwo(spsAfterEndOfPause);

		allSpsAfterBeginOfPause.forEach(InterfaceWrapperHelper::save);
	}

	private void createBeginOfPause(
			@NonNull final I_C_Flatrate_Term term,
			@NonNull final Timestamp pauseFrom,
			final int seqNoOfPauseRecord)
	{
		final I_C_SubscriptionProgress pauseBegin = newInstance(I_C_SubscriptionProgress.class);

		pauseBegin.setEventType(X_C_SubscriptionProgress.EVENTTYPE_Abopause_Beginn);
		pauseBegin.setC_Flatrate_Term(term);
		pauseBegin.setStatus(X_C_SubscriptionProgress.STATUS_Geplant);
		pauseBegin.setContractStatus(X_C_SubscriptionProgress.CONTRACTSTATUS_Lieferpause);
		pauseBegin.setEventDate(pauseFrom);
		pauseBegin.setSeqNo(seqNoOfPauseRecord);
		save(pauseBegin);
	}

	private ImmutableList<I_C_SubscriptionProgress> updateAndCollectRecordWithinPause(
			@NonNull final List<I_C_SubscriptionProgress> sps,
			@NonNull final Timestamp pauseUntil)
	{
		final Builder<I_C_SubscriptionProgress> spsWithinPause = ImmutableList.builder();

		for (final I_C_SubscriptionProgress sp : sps)
		{
			if (sp.getEventDate().after(pauseUntil))
			{
				continue;
			}
			spsWithinPause.add(sp);
			sp.setSeqNo(sp.getSeqNo() + 1);

			if (Objects.equals(sp.getStatus(), X_C_SubscriptionProgress.STATUS_Geplant))
			{
				sp.setContractStatus(X_C_SubscriptionProgress.CONTRACTSTATUS_Lieferpause);
			}
		}

		return spsWithinPause.build();
	}

	private static int computeEndOfPauseSeqNo(final I_C_SubscriptionProgress firstSpAfterBeginOfPause, final ImmutableList<I_C_SubscriptionProgress> spsWithinPause)
	{
		return spsWithinPause.isEmpty() ? firstSpAfterBeginOfPause.getSeqNo() + 1 : spsWithinPause.get(spsWithinPause.size() - 1).getSeqNo() + 1;
	}

	private void createEndOfPause(
			final I_C_Flatrate_Term term,
			final Timestamp pauseUntil,
			final int seqNoOfPauseRecord)
	{
		final I_C_SubscriptionProgress pauseEnd = newInstance(I_C_SubscriptionProgress.class);

		pauseEnd.setEventType(X_C_SubscriptionProgress.EVENTTYPE_Abopause_Ende);
		pauseEnd.setC_Flatrate_Term(term);
		pauseEnd.setStatus(X_C_SubscriptionProgress.STATUS_Geplant);
		pauseEnd.setContractStatus(X_C_SubscriptionProgress.CONTRACTSTATUS_Laufend);
		pauseEnd.setEventDate(pauseUntil);
		pauseEnd.setSeqNo(seqNoOfPauseRecord);
		save(pauseEnd);
	}

	private ImmutableList<I_C_SubscriptionProgress> collectSpsAfterEndOfPause(final List<I_C_SubscriptionProgress> sps, Timestamp pauseUntil)
	{
		final ImmutableList<I_C_SubscriptionProgress> spsAfterPause = sps.stream()
				.filter(sp -> sp.getEventDate().after(pauseUntil))
				.collect(ImmutableList.toImmutableList());
		return spsAfterPause;
	}

	private void increaseSeqNosByTwo(@NonNull final List<I_C_SubscriptionProgress> sps)
	{
		final int seqNoOffSet = 2;
		for (final I_C_SubscriptionProgress currentSP : sps)
		{
			currentSP.setSeqNo(currentSP.getSeqNo() + seqNoOffSet);
		}
	}
}
