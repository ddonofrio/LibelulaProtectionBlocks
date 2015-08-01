/*
 *            This file is part of LibelulaProtectionBlocks.
 *
 *  LibelulaProtectionBlocks is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibelulaProtectionBlocks is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibelulaProtectionBlocks. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.pb;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.bukkit.ChatColor;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class TextManager {

    private final Main plugin;
    private Locale currentLocale;
    private ResourceBundle messages;

    public TextManager(Main plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws MalformedURLException {
        String lang = plugin.getConfig().getString("lang");
        String country = plugin.getConfig().getString("country");
        currentLocale = new Locale(lang, country);
        File i18nFolder;
        i18nFolder = new File(plugin.getDataFolder(), "lang");
        if (!i18nFolder.exists()) {
            i18nFolder.mkdirs();
        }
        URL[] urls = new URL[]{i18nFolder.toURI().toURL()};
        ClassLoader loader = new URLClassLoader(urls);
        
        File selectedLang = new File(i18nFolder, "i18n_" + lang.toLowerCase() 
                + "_" + country.toUpperCase() + ".properties");
        String langFileName = "lang/" + selectedLang.getName();
        
        if (plugin.getResource(langFileName) != null) {
            plugin.saveResource(langFileName, true);
        } else if (!new File (plugin.getDataFolder(), langFileName).exists()) {
            plugin.alert("Invalid configured lang/country, setting to en/US");
            plugin.saveResource("lang/i18n_en_US.properties", true);
            currentLocale = new Locale("en", "US");
        }
        messages = ResourceBundle.getBundle("i18n", currentLocale, loader);        
        plugin.getLogger().info(getText("i18n_selection"));
    }
    
    public String getText(String text, Object... params  ) {
        String result = text;
        try {
            result = MessageFormat.format(messages.getString(text), params);
        } catch (MissingResourceException e) {
            plugin.sendMessage(plugin.getServer().getConsoleSender(),
                    ChatColor.RED + "Translation not found: \"" + result + "\"");
        } catch (NullPointerException e) {
            plugin.sendMessage(plugin.getServer().getConsoleSender(),
                    ChatColor.RED + "Text manager not initialized.");
        }
        return result;
    }
    
    public boolean isInitialized() {
        return messages != null;
    }
    
}
