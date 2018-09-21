package de.metas.picking.service;

/*
 * #%L
 * de.metas.swat.base
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.adempiere.exceptions.AdempiereException;

import de.metas.picking.legacy.form.IPackingItem;
import lombok.NonNull;
import lombok.ToString;

/**
 * This map helps to keep track about which item is packed into which place..it's sort of legacy..
 * <p>
 * As far as i see, it needs to be initialized using {@link #addUnpackedItem(IPackingItem)} or {@link #addUnpackedItems(Collection)}, before the fun can start.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
@ToString
public final class PackingItemsMap
{
	public static PackingItemsMap ofUnpackedItems(final Collection<IPackingItem> unpackedItems)
	{
		PackingItemsMap map = new PackingItemsMap();
		map.addUnpackedItems(unpackedItems);
		return map;
	}

	public static PackingItemsMap ofUnpackedItem(@NonNull final IPackingItem unpackedItem)
	{
		PackingItemsMap map = new PackingItemsMap();
		map.addUnpackedItem(unpackedItem);
		return map;
	}

	private final Map<PackingItemsMapKey, List<IPackingItem>> itemsMap = new HashMap<>();

	public PackingItemsMap()
	{
		final List<IPackingItem> unpackedItems = new ArrayList<>();
		itemsMap.put(PackingItemsMapKey.UNPACKED, unpackedItems);
	}

	/** Copy constructor */
	private PackingItemsMap(final PackingItemsMap copyFrom)
	{
		//
		// Do a deep-copy of "copyFrom"'s map
		for (final Entry<PackingItemsMapKey, List<IPackingItem>> key2itemsList : copyFrom.itemsMap.entrySet())
		{
			final List<IPackingItem> items = key2itemsList.getValue();

			// skip null items (shall not happen)
			if (items == null)
			{
				continue;
			}

			// NOTE: to avoid NPEs we are also copying empty lists

			final List<IPackingItem> itemsCopy = new ArrayList<>(items);

			final PackingItemsMapKey key = key2itemsList.getKey();

			itemsMap.put(key, itemsCopy);
		}
	}

	public List<IPackingItem> get(final PackingItemsMapKey key)
	{
		return itemsMap.get(key);
	}

	private void addUnpackedItems(final Collection<? extends IPackingItem> unpackedItemsToAdd)
	{
		if (unpackedItemsToAdd == null || unpackedItemsToAdd.isEmpty())
		{
			return;
		}

		final List<IPackingItem> unpackedItems = getUnpackedItems();
		unpackedItems.addAll(unpackedItemsToAdd);
	}

	public final List<IPackingItem> getUnpackedItems()
	{
		return itemsMap.computeIfAbsent(PackingItemsMapKey.UNPACKED, k -> new ArrayList<>());
	}

	public void addUnpackedItem(@NonNull final IPackingItem unpackedItemToAdd)
	{
		final List<IPackingItem> unpackedItems = getUnpackedItems();
		unpackedItems.add(unpackedItemToAdd);
	}

	public void put(final PackingItemsMapKey key, final List<IPackingItem> items)
	{
		itemsMap.put(key, items);
	}

	public Set<Entry<PackingItemsMapKey, List<IPackingItem>>> entrySet()
	{
		return itemsMap.entrySet();
	}

	public PackingItemsMap copy()
	{
		return new PackingItemsMap(this);
	}

	public List<IPackingItem> remove(final PackingItemsMapKey key)
	{
		return itemsMap.remove(key);
	}

	public void removeUnpackedItem(@NonNull final IPackingItem itemToRemove)
	{
		final List<IPackingItem> unpackedItems = getUnpackedItems();
		for (final Iterator<IPackingItem> it = unpackedItems.iterator(); it.hasNext();)
		{
			final IPackingItem item = it.next();
			if (item.isSameAs(itemToRemove))
			{
				it.remove();
				return;
			}
		}

		throw new AdempiereException("Unpacked item " + itemToRemove + " was not found in: " + unpackedItems);
	}

	/**
	 * Append given <code>itemPacked</code> to existing packed items
	 *
	 * @param key
	 * @param itemPacked
	 */
	public void appendPackedItem(final PackingItemsMapKey key, final IPackingItem itemPacked)
	{
		List<IPackingItem> existingPackedItems = get(key);
		if (existingPackedItems == null)
		{
			existingPackedItems = new ArrayList<>();
			put(key, existingPackedItems);
		}
		else
		{
			for (final IPackingItem item : existingPackedItems)
			{
				// add new item into the list only if is a real new item
				// NOTE: should be only one item with same grouping key
				if (item.getGroupingKey() == itemPacked.getGroupingKey())
				{
					item.addSchedules(itemPacked);
					return;
				}
			}
		}

		//
		// No matching existing packed item where our item could be added was found
		// => add it here as a new item
		existingPackedItems.add(itemPacked);
	}

	/**
	 *
	 * @return true if there exists at least one packed item
	 */
	public boolean hasPackedItems()
	{
		for (final Entry<PackingItemsMapKey, List<IPackingItem>> key2itemsList : itemsMap.entrySet())
		{
			final PackingItemsMapKey key = key2itemsList.getKey();
			if (key.isUnpacked())
			{
				continue;
			}

			final List<IPackingItem> items = key2itemsList.getValue();
			if (items == null || items.isEmpty())
			{
				continue;
			}

			// if we reach this point it means that we just found a list with packed items
			return true;
		}

		return false;
	}

	/**
	 *
	 * @return true if there exists at least one unpacked item
	 */
	public boolean hasUnpackedItems()
	{
		final List<IPackingItem> unpackedItems = getUnpackedItems();
		if (unpackedItems == null || unpackedItems.isEmpty())
		{
			return false;
		}

		return true;
	}
}
