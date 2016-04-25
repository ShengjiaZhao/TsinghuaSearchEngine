

设置爬虫的seed
```
 <bean id="longerOverrides" class="org.springframework.beans.factory.config.PropertyOverrideConfigurer">
  	<property name="properties">
   		<props>
    		<prop key="seeds.textSource.value">
			http://news.tsinghua.edu.cn/
			http://info.tsinghua.edu.cn/
    		</prop>
   		</props>
  	</property>
 </bean>
```
设置搜索的范围，具体为拒绝所有链接，除非这些链接属于tsinghua.edu.cn域，或者从tsinghua.edu.cn域直接指向．但是不爬取过长，以某些特定格式结尾的链接，如mpeg，或者不能正确解析的链接．
```
 <bean id="acceptSurts" class="org.archive.modules.deciderules.surt.SurtPrefixedDecideRule">
 	<property name="decision" value="ACCEPT"/> 
 	<property name="seedsAsSurtPrefixes" value="true" /> 
 	<property name="surtsDumpFile" value="${launchId}/surts.dump" /> 
  	<property name="surtsSource">
    	<bean class="org.archive.spring.ConfigString">
        <property name="value">
        <value>
      		http://cn,edu,tsinghua
        </value>
        </property> 
        </bean>
     </property> 
 </bean>

<bean id="scope" class="org.archive.modules.deciderules.DecideRuleSequence">
  	<property name="rules">
   	<list>
    <!-- Begin by REJECTing all... -->
    <bean class="org.archive.modules.deciderules.RejectDecideRule" />
    <!-- ...then ACCEPT those within configured/seed-implied SURT prefixes... -->
    <ref bean="acceptSurts" />
    <!-- ...but REJECT those more than a configured link-hop-count from start... -->
    <bean class="org.archive.modules.deciderules.TooManyHopsDecideRule">
      	<property name="maxHops" value="20" /> 
    </bean>
    <!-- ...but ACCEPT those more than a configured link-hop-count from start... -->
    <bean class="org.archive.modules.deciderules.TransclusionDecideRule">
      	<property name="maxTransHops" value="2" /> 
      	<property name="maxSpeculativeHops" value="1" /> 
    </bean>
    <!-- ...but REJECT those from a configurable (initially empty) set of REJECT SURTs... -->
    <bean class="org.archive.modules.deciderules.surt.SurtPrefixedDecideRule">
		 <property name="decision" value="REJECT"/>
		 <property name="seedsAsSurtPrefixes" value="false"/>
		 <property name="surtsDumpFile" value="${launchId}/negative-surts.dump" /> 
		 <property name="surtsSource">
          	<bean class="org.archive.spring.ConfigString">
         	<property name="value">
          	<value>
      			http://cn,edu,tsinghua,lib
				166.111.120.
         	</value>
         	</property> 
        	</bean>
 		</property> 
    </bean>
    <!-- ...and REJECT those with suspicious repeating path-segments... -->
    <bean class="org.archive.modules.deciderules.PathologicalPathDecideRule">
     <!-- <property name="maxRepetitions" value="2" /> -->
    </bean>
    <!-- ...and REJECT those with more than threshold number of path-segments... -->
    <bean class="org.archive.modules.deciderules.TooManyPathSegmentsDecideRule">
     <!-- <property name="maxPathDepth" value="20" /> -->
    </bean>
     <!-- ...and REJECT those with more than threshold number of path-segments... -->
    <bean class="org.archive.modules.deciderules.MatchesRegexDecideRule">
      	<property name="decision" value="REJECT"/> 
     	<property name="regex" value=".*(?i)(\.(mso|tar|txt|asx|asf|bz2|mpe?g|MPE?G|flv|FLV|tiff?|gif|GIF|png|PNG|ico|ICO|css|sit|eps|wmf|zip|pptx?|xlsx?|gz|rpm|tgz|mov|MOV|exe|jpe?g|JPE?G|bmp|BMP|docx?|DOCX?|rar|RAR|jar|JAR|ZIP|zip|gz|GZ|wma|WMA|rm|RM|rmvb|RMVB|avi|AVI|swf|SWF|mp3|MP3|wmv|WMV))$"/> 
    </bean> 
    <!-- ...but always ACCEPT those marked as prerequisitee for another URI... -->
    <bean class="org.archive.modules.deciderules.PrerequisiteAcceptDecideRule">
    </bean>
    <!-- ...but always REJECT those with unsupported URI schemes -->
    <bean class="org.archive.modules.deciderules.SchemeNotInSetDecideRule">
    </bean>
   	</list>
  	</property>
 </bean>
```
最后设置存储的方式为镜像，并且只存储支持的格式(htm, html, pdf)
```
 <bean id="warcWriter" class="org.archive.modules.writer.MirrorWriterProcessor">
     <property name="shouldProcessRule">
     <bean class="org.archive.modules.deciderules.DecideRuleSequence">
       <property name="rules">
         <list>
           <!-- Begin by REJECTing all... -->
           <bean class="org.archive.modules.deciderules.RejectDecideRule" />
           <!-- ...but accept those with a text/html mime type -->
           <bean class="org.archive.modules.deciderules.MatchesRegexDecideRule">
             <property name="decision" value="ACCEPT" />
             <property name="regex" value=".*(?i)(\.(html|htm|pdf))" />          
            </bean>
         </list>
       </property>
     </bean>
   </property>
</bean>
```
