package de.metas.handlingunits.attribute.strategy;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import org.compiere.model.I_M_Attribute;

public interface IHUAttributeTransferStrategy extends IAttributeStrategy
{
	/**
	 * Transfer attribute between storages for given request.
	 *
	 * @param request
	 * @param attribute
	 */
	void transferAttribute(IHUAttributeTransferRequest request, I_M_Attribute attribute);

	/**
	 * @param request
	 * @param attribute
	 * @return true if the value of the given attribute can be transferred for this the request
	 */
	boolean isTransferable(IHUAttributeTransferRequest request, I_M_Attribute attribute);
}