/**
 * **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2011 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalVariableBehavior;

/**
 * Concurrent type local context provider.
 * Ruby runtime returned from the getRuntime() method is a classloader-global runtime.
 * While variables (except global variables) and constants are thread local.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class ConcurrentLocalContextProvider extends AbstractLocalContextProvider {
    private ThreadLocal<LocalContext> contextHolder =
            new ThreadLocal<LocalContext>() {
                @Override
                public LocalContext initialValue() {
                    return getInstance();
                }
                
                @Override
                public void remove() {
                    LocalContext localContext = get();
                    localContext.remove();
                }
            };

    public ConcurrentLocalContextProvider(LocalVariableBehavior behavior, boolean lazy) {
        // To save startup time, Ruby runtime instantiation should be delayed as mush as possible
        // so, don't create runtime here.
        if (Ruby.isGlobalRuntimeReady()) config = Ruby.getGlobalRuntime().getInstanceConfig();
        else config = new RubyInstanceConfig();
        this.behavior = behavior;
        this.lazy = lazy;
    }
    
    public Ruby getRuntime() {
        if (!Ruby.isGlobalRuntimeReady()) {
            return Ruby.newInstance(config);
        }
        return Ruby.getGlobalRuntime();
    }

    @Override
    public RubyInstanceConfig getRubyInstanceConfig() {
        if (Ruby.isGlobalRuntimeReady()) return Ruby.getGlobalRuntime().getInstanceConfig();
        else return config;
    }

    public BiVariableMap getVarMap() {
        return contextHolder.get().getVarMap(this);
    }

    public Map getAttributeMap() {
        return contextHolder.get().getAttributeMap();
    }

    public boolean isRuntimeInitialized() {
        return Ruby.isGlobalRuntimeReady();
    }
    
    public void terminate() {
        contextHolder.remove();
        contextHolder.set(null);
    }
}
