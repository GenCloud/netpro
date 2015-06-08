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
package net.l2emuproject.proxy.network.meta.condition;

/**
 * Tests whether each of required bits is set.
 * 
 * @author _dev_
 */
public abstract class BitmaskCondition implements IntegerCondition
{
	private final long _mask;
	
	/**
	 * Constructs this condition.
	 * 
	 * @param mask bits required to be set
	 */
	protected BitmaskCondition(long mask)
	{
		_mask = mask;
	}
	
	@Override
	public boolean test(long value)
	{
		return (value & _mask) == _mask;
	}
}
