/*
 * Copyright 2011-2015 L2EMU UNIQUE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.l2emuproject.proxy.network.meta.structure.field;

import net.l2emuproject.util.HexUtil;

/**
 * A simple base class that stores values.
 * 
 * @author _dev_
 */
public abstract class AbstractFieldValue implements FieldValue
{
	private final byte[] _raw;
	private final Object _interpreted;
	
	/**
	 * Stores given values.
	 * 
	 * @param raw raw value
	 * @param interpreted value interpretation
	 */
	protected AbstractFieldValue(byte[] raw, Object interpreted)
	{
		_raw = raw;
		_interpreted = interpreted;
	}
	
	@Override
	public byte[] raw()
	{
		return _raw;
	}
	
	@Override
	public Object interpreted()
	{
		return _interpreted;
	}
	
	@Override
	public String toString()
	{
		return HexUtil.bytesToHexString(_raw, " ");
	}
}
