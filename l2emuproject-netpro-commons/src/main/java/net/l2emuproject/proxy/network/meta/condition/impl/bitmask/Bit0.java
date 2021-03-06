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
package net.l2emuproject.proxy.network.meta.condition.impl.bitmask;

import net.l2emuproject.proxy.network.meta.condition.BitmaskCondition;

/**
 * Tests whether the value of a byte/word/dword/qword has the least significant bit set.
 * 
 * @author savormix
 */
public final class Bit0 extends BitmaskCondition
{
	/** Constructs this condition. */
	public Bit0()
	{
		super(1 << 0);
	}
}
