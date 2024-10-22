/*
 * Copyright (C) 2024 hstr0100
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.brlns.gdownloader.settings.filters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.brlns.gdownloader.GDownloader;
import net.brlns.gdownloader.settings.QualitySettings;
import net.brlns.gdownloader.settings.enums.DownloadTypeEnum;

/**
 * @author Gabriel / hstr0100 / vertx010
 */
@Data
@Slf4j
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "Id",
    defaultImpl = GenericFilter.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = YoutubeFilter.class, name = YoutubeFilter.ID),
    @JsonSubTypes.Type(value = YoutubePlaylistFilter.class, name = YoutubePlaylistFilter.ID),

    @JsonSubTypes.Type(value = BiliBiliFilter.class, name = BiliBiliFilter.ID),
    @JsonSubTypes.Type(value = CrunchyrollFilter.class, name = CrunchyrollFilter.ID),
    @JsonSubTypes.Type(value = DailymotionFilter.class, name = DailymotionFilter.ID),
    @JsonSubTypes.Type(value = DropoutFilter.class, name = DropoutFilter.ID),
    @JsonSubTypes.Type(value = FacebookFilter.class, name = FacebookFilter.ID),
    @JsonSubTypes.Type(value = ImgurFilter.class, name = ImgurFilter.ID),
    @JsonSubTypes.Type(value = RedditFilter.class, name = RedditFilter.ID),
    @JsonSubTypes.Type(value = TwitchFilter.class, name = TwitchFilter.ID),
    @JsonSubTypes.Type(value = VimeoFilter.class, name = VimeoFilter.ID),
    @JsonSubTypes.Type(value = XFilter.class, name = XFilter.ID),

    @JsonSubTypes.Type(value = GenericFilter.class, name = GenericFilter.ID)
})
public abstract class AbstractUrlFilter{

    @JsonIgnore
    private static final List<Class<?>> DEFAULTS = new ArrayList<>();

    static{
        JsonSubTypes jsonSubTypes = AbstractUrlFilter.class.getAnnotation(JsonSubTypes.class);

        if(jsonSubTypes != null){
            JsonSubTypes.Type[] types = jsonSubTypes.value();

            for(JsonSubTypes.Type type : types){
                DEFAULTS.add(type.value());
            }
        }
    }

    @JsonIgnore
    public static List<AbstractUrlFilter> getDefaultUrlFilters(){
        List<AbstractUrlFilter> filters = new ArrayList<>();

        for(Class<?> filterClass : DEFAULTS){
            try{
                AbstractUrlFilter filter = (AbstractUrlFilter)filterClass.getDeclaredConstructor().newInstance();

                filters.add(filter);
            }catch(Exception e){
                log.error("Error instantiating class.", e);
            }
        }

        return filters;
    }

    @JsonProperty("Id")
    private String id = "";

    @JsonProperty("FilterName")
    private String filterName = "";

    @JsonProperty("UrlRegex")
    private String urlRegex = "";

    @JsonProperty("VideoNamePattern")
    private String videoNamePattern = "";

    @JsonProperty("AudioNamePattern")
    private String audioNamePattern = "";

    @JsonProperty("EmbedThumbnailAndMetadata")
    private boolean embedThumbnailAndMetadata = false;

    /**
     * Represents a set of extra arguments for yt-dlp.
     * These arguments are categorized based on the type of download (e.g., VIDEO, AUDIO, SUBTITLES, etc.).
     * Arguments in the ALL category apply to all categories that depend on this filter.
     *
     * JSON schema:
     *
     * <pre>
     * "ExtraYtDlpArguments" : {
     *   "ALL": [
     *     "--ignore-config",
     *     "--proxy",
     *     "http://example.com:1234",
     *     "--skip-download"
     *   ],
     *   "VIDEO": [
     *     "--no-playlist"
     *   ],
     *   "AUDIO": [],
     *   "SUBTITLES" : [],
     *   "THUMBNAILS" : []
     * }
     * </pre>
     */
    @JsonProperty("ExtraYtDlpArguments")
    private Map<DownloadTypeEnum, List<String>> extraYtDlpArguments = new HashMap<>();

    @JsonProperty("QualitySettings")
    private QualitySettings qualitySettings = QualitySettings.builder().build();

    public AbstractUrlFilter(){
        for(DownloadTypeEnum downloadType : DownloadTypeEnum.values()){
            extraYtDlpArguments.put(downloadType, new ArrayList<>());
        }
    }

    @JsonIgnore
    public String getDisplayName(){
        String name = getFilterName();
        if(name.isEmpty()){
            log.error("Filter name was empty for class: {}", getClass());
        }

        return name;
    }

    @JsonIgnore
    private Pattern _cachedPattern;

    @JsonIgnore
    public boolean matches(String url){
        if(urlRegex.isEmpty()){
            return false;
        }

        if(_cachedPattern == null){
            _cachedPattern = Pattern.compile(urlRegex);
        }

        return _cachedPattern.matcher(url).matches();
    }

    @JsonIgnore
    public List<String> getArguments(DownloadTypeEnum typeEnum, GDownloader main, File savePath){
        List<String> arguments = new ArrayList<>();

        arguments.addAll(buildArguments(typeEnum, main, savePath));

        if(extraYtDlpArguments.containsKey(typeEnum)){
            arguments.addAll(extraYtDlpArguments.get(typeEnum));
        }

        return arguments;
    }

    @JsonIgnore
    protected abstract List<String> buildArguments(DownloadTypeEnum typeEnum, GDownloader main, File savePath);

    @JsonIgnore
    public abstract boolean areCookiesRequired();

    @JsonIgnore
    public abstract boolean canAcceptUrl(String url, GDownloader main);

}
