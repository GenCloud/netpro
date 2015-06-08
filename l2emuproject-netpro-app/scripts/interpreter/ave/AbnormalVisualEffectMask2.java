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
package interpreter.ave;

import java.util.Arrays;

import eu.revengineer.simplejse.HasScriptDependencies;

import net.l2emuproject.proxy.script.interpreter.ScriptedBitmaskInterpreter;

/**
 * Interprets the given bit mask as displayed abnormal visual effects (extension field).
 * 
 * @author _dev_
 */
@HasScriptDependencies("interpreter.ave.IAbnormalVisualEffect")
public class AbnormalVisualEffectMask2 extends ScriptedBitmaskInterpreter implements IAbnormalVisualEffect
{
	/** Constructs this interpreter. */
	public AbnormalVisualEffectMask2()
	{
		super(Arrays.copyOfRange(EFFECTS, Math.min(EFFECTS.length, 32), Math.min(EFFECTS.length, 64), Object[].class));
	}
}
