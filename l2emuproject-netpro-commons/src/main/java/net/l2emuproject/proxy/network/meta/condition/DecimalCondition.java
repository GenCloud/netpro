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

import net.l2emuproject.proxy.network.meta.FieldValueCondition;

/**
 * A condition for floating point values.
 * 
 * @author _dev_
 */
public interface DecimalCondition extends FieldValueCondition
{
	/**
	 * Tests whether the given value satisfies this condition.
	 * 
	 * @param value floating point value
	 * @return is condition met
	 */
	boolean test(double value);
}
