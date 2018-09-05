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
package interpreter.equip;

import net.l2emuproject.proxy.script.interpreter.ScriptedBitmaskInterpreter;

/**
 * Interprets the fifth slot mask byte of the ExUIEquipSlot packet.
 * 
 * @author _dev_
 */
public class ExUserEquipSlot5EF extends ScriptedBitmaskInterpreter
{
	/** Constructs this interpreter. */
	public ExUserEquipSlot5EF()
	{
		super("Balance artifact (1)", "Book", "Jewel (6)", "Jewel (5)", "Jewel (4)", "Jewel (3)", "Jewel (2)", "Jewel (1)");
	}
}
