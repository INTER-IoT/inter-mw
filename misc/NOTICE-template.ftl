INTER-IoT Interoperability of Heterogeneous IoT Platforms
INTER-MW Interoperability middleware

This project has received funding from the European Union's Horizon 2020
research and innovation programme under grant agreement No 687283.

Copyright 2016-2018 Universitat Politècnica de València

Copyright 2016-2018 Università della Calabria

Copyright 2016-2018 Prodevelop, SL

Copyright 2016-2018 Technische Universiteit Eindhoven

Copyright 2016-2018 Fundación de la Comunidad Valenciana para la
Investigación, Promoción y Estudios Comerciales de Valenciaport

Copyright 2016-2018 Rinicom Ltd

Copyright 2016-2018 Association pour le développement de la formation
professionnelle dans le transport

Copyright 2016-2018 Noatum Ports Valenciana, S.A.U.

Copyright 2016-2018 XLAB razvoj programske opreme in svetovanje d.o.o.

Copyright 2016-2018 Systems Research Institute Polish Academy of Sciences

Copyright 2016-2018 Azienda Sanitaria Locale TO5

Copyright 2016-2018 Alessandro Bassi Consulting SARL

Copyright 2016-2018 Neways Technologies B.V.

I. Used Software

This product uses the following software with corresponding licenses:

<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#assign result = result + license/>
        <#if license_has_next>
            <#assign result = result + " or "/>
        </#if>
    </#list>
    <#return result>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>
<#if dependencyMap?size == 0>
The project has no dependencies.
<#else>
    <#list dependencyMap as e>
        <#assign project = e.getKey()/>
        <#assign licenses = e.getValue()/>
        ${artifactFormat(project)} distributed under ${licenseFormat(licenses)}
    </#list>
</#if>