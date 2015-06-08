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
package condition.param;

import net.l2emuproject.proxy.network.meta.condition.IntegerCondition;
import net.l2emuproject.proxy.script.condition.ScriptedFieldValueCondition;

/**
 * Tests whether the value of a byte/word/dword is equal to 19 or 20.<BR>
 * <BR>
 * Used to check the type of parameter in <TT>SystemMessage</TT>.
 * 
 * @author _dev_
 */
public class TinyNumberValue extends ScriptedFieldValueCondition implements IntegerCondition
{
	@Override
	public boolean test(long value)
	{
		// 19 is used for enchant values, 20 measures time (min/sec)
		return value == 19 || value == 20;
	}
}
