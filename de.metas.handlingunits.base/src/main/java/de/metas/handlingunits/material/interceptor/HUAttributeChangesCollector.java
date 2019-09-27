package de.metas.handlingunits.material.interceptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.AttributeSetInstanceId;
import org.adempiere.mm.attributes.api.AttributesKeys;
import org.adempiere.warehouse.WarehouseId;
import org.adempiere.warehouse.api.IWarehouseDAO;

import com.google.common.collect.ImmutableList;

import de.metas.handlingunits.HuId;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.storage.IHUProductStorage;
import de.metas.material.event.PostMaterialEventService;
import de.metas.material.event.attributes.AttributesChangedEvent;
import de.metas.material.event.attributes.AttributesKeyWithASI;
import de.metas.material.event.commons.AttributesKey;
import de.metas.material.event.commons.EventDescriptor;
import de.metas.util.Check;
import de.metas.util.Services;
import lombok.NonNull;

/*
 * #%L
 * de.metas.handlingunits.base
 * %%
 * Copyright (C) 2019 metas GmbH
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

final class HUAttributeChangesCollector
{
	// services
	private final IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
	private final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
	private final IWarehouseDAO warehousesRepo = Services.get(IWarehouseDAO.class);
	private final PostMaterialEventService materialEventService;

	// state
	private final AtomicBoolean disposed = new AtomicBoolean();
	private final HashMap<HuId, HUAttributeChanges> huAttributeChangesMap = new HashMap<>();

	private final HashMap<AttributesKey, AttributesKeyWithASI> attributesKeyWithASIsCache = new HashMap<>();

	public HUAttributeChangesCollector(@NonNull final PostMaterialEventService materialEventService)
	{
		this.materialEventService = materialEventService;
	}

	public void collect(@NonNull final HUAttributeChange change)
	{
		Check.assume(!disposed.get(), "Collector shall not be disposed: {}", this);
		final HUAttributeChanges huChanges = huAttributeChangesMap.computeIfAbsent(change.getHuId(), HUAttributeChanges::new);
		huChanges.collect(change);
	}

	public void createAndPostMaterialEvents()
	{
		if (disposed.getAndSet(true))
		{
			throw new AdempiereException("Collector was already disposed: " + this);
		}

		final List<AttributesChangedEvent> events = new ArrayList<>();
		for (final HUAttributeChanges huAttributeChanges : huAttributeChangesMap.values())
		{
			events.addAll(createMaterialEvent(huAttributeChanges));
		}

		materialEventService.postEventsNow(events);
	}

	private List<AttributesChangedEvent> createMaterialEvent(final HUAttributeChanges changes)
	{
		if (changes.isEmpty())
		{
			return ImmutableList.of();
		}

		final HuId huId = changes.getHuId();
		final I_M_HU hu = handlingUnitsDAO.getById(huId);

		final EventDescriptor eventDescriptor = EventDescriptor.ofClientAndOrg(hu.getAD_Client_ID(), hu.getAD_Org_ID());
		final Instant date = changes.getLastChangeDate();
		final WarehouseId warehouseId = warehousesRepo.getWarehouseIdByLocatorRepoId(hu.getM_Locator_ID());

		final AttributesKeyWithASI oldStorageAttributes = toAttributesKeyWithASI(changes.getOldAttributesKey());
		final AttributesKeyWithASI newStorageAttributes = toAttributesKeyWithASI(changes.getNewAttributesKey());

		final List<IHUProductStorage> productStorages = handlingUnitsBL.getStorageFactory()
				.getStorage(hu)
				.getProductStorages();

		final List<AttributesChangedEvent> events = new ArrayList<>();
		for (IHUProductStorage productStorage : productStorages)
		{
			events.add(AttributesChangedEvent.builder()
					.eventDescriptor(eventDescriptor)
					.warehouseId(warehouseId)
					.date(date)
					.productId(productStorage.getProductId().getRepoId())
					.qty(productStorage.getQtyInStockingUOM().toBigDecimal())
					.oldStorageAttributes(oldStorageAttributes)
					.newStorageAttributes(newStorageAttributes)
					.huId(productStorage.getHuId().getRepoId())
					.build());
		}

		return events;
	}

	private AttributesKeyWithASI toAttributesKeyWithASI(final AttributesKey attributesKey)
	{
		return attributesKeyWithASIsCache.computeIfAbsent(attributesKey, this::createAttributesKeyWithASI);
	}

	private AttributesKeyWithASI createAttributesKeyWithASI(final AttributesKey attributesKey)
	{
		final AttributeSetInstanceId asiId = AttributesKeys.createAttributeSetInstanceFromAttributesKey(attributesKey);
		return AttributesKeyWithASI.of(attributesKey, asiId);
	}
}
