<?xml version="1.0"?>
<!--

  Copyright (C) 2012 jOVAL.org.  All rights reserved.
  This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

-->
<xsl:stylesheet xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:xccdf="http://checklists.nist.gov/xccdf/1.2"
                xmlns:diagnostic="http://www.joval.org/schemas/scap/1.2/diagnostic">
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>XCCDF Scan Results</title>
        <xsl:call-template name="DetailJsCss" />
      </head>
      <body>

        <h1>XCCDF Scan Results</h1>
        <table class="keyvalue striped">
          <tr>
            <td>Scan Date</td>
            <td>Started 
                <xsl:call-template name="printDateTime">
                  <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@start-time"/>
                </xsl:call-template> 
                and completed 
                <xsl:call-template name="printDateTime">
                  <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@end-time"/>
                </xsl:call-template> 
            </td>
          </tr>
          <tr>
            <td>Benchmark</td>
            <td>
              <xsl:value-of select="/xccdf:Benchmark/xccdf:title/text()"/>
              version <xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/@version"/>
              <small> <xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/xccdf:benchmark/@id"/></small>
            </td>
          </tr>
          <tr>
            <td>Profile</td>
            <td>
              <xsl:variable name="profileId"><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/xccdf:profile/@idref"/></xsl:variable>
              <xsl:value-of select="/xccdf:Benchmark/xccdf:Profile[@id=$profileId]/xccdf:title/text()"/> <small> <xsl:value-of select="$profileId"/></small>
            </td>
          </tr>
          <tr>
            <td>Target</td>
            <td>
              <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:target">
                <xsl:value-of select="./text()"/><xsl:if test="position() != last()">, </xsl:if>
              </xsl:for-each>
              <small>
                <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:target-address">
                  <xsl:sort select="./text()" data-type="text" order="ascending"/>
                  <xsl:value-of select="./text()"/>
                  <xsl:if test="position() != last()">, </xsl:if>
                </xsl:for-each>
              </small>
            </td>
          </tr>
          <tr>
            <td>Identity</td>
            <td>
              <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:identity">
                <xsl:value-of select="./text()"/>
                <small>
                  <xsl:if test="./@authenticated = 'true'"> authenticated, </xsl:if>
                  <xsl:if test="./@authenticated = 'false'"> not authenticated, </xsl:if>
                  <xsl:if test="./@privileged = 'true'">privileged</xsl:if>
                  <xsl:if test="./@privileged = 'false'">not privileged</xsl:if>
                </small><xsl:if test="position() != last()"><br /></xsl:if>
              </xsl:for-each>            
            </td>
          </tr>
          <tr>
            <td>System</td>
            <td><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/@test-system"/></td>
          </tr>
        </table>

        <h2>Scoring</h2>
        <table class="scores striped">
          <tr class="header">
            <td class="method">Method</td>
            <td class="score">Score</td>
            <td class="maximum">Max</td>
            <td class="percentage">%</td>
          </tr>
          <xsl:choose>
            <xsl:when test="//xccdf:Benchmark/xccdf:TestResult/xccdf:score">
              <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:score">
                <tr>
                  <td class="method">
                    <xsl:choose>
                      <xsl:when test="./@system = 'urn:xccdf:scoring:default'">Default Scoring</xsl:when>
                      <xsl:when test="./@system = 'urn:xccdf:scoring:flat'">Flat Scoring</xsl:when>
                      <xsl:when test="./@system = 'urn:xccdf:scoring:flat-unweighted'">Flat Unweighted Scoring</xsl:when>
                      <xsl:when test="./@system = 'urn:xccdf:scoring:absolute'">Absolute Scoring</xsl:when>
                      <xsl:otherwise><xsl:value-of select="./@system"/></xsl:otherwise>
                    </xsl:choose>
                  </td>
                  <td class="score"><xsl:value-of select="format-number(./text(),'###,##0.00')"/></td>
                  <td class="maximum"><xsl:value-of select="format-number(./@maximum,'###,##0.00')"/></td>
                  <td class="percentage"><xsl:value-of select='format-number((./text() div ./@maximum), "0.00%")' /></td>
                </tr>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <tr><td class="method">No scoring data found in result.</td><td class="score"></td><td class="maximum"></td><td class="percentage"></td></tr>
            </xsl:otherwise>
          </xsl:choose>
        </table>

        <h2>Rule Results</h2>
        <table class="rule-results">
          <tr class="header">
            <td class="rule-title">Rule</td>
            <td class="identifiers">Reference(s)</td>
            <td class="rule-result">Result</td>
          </tr>

          <xsl:for-each select="/xccdf:Benchmark/xccdf:Group">
            <xsl:call-template name="GroupResult">
              <xsl:with-param name="groupElt" select="."/>
            </xsl:call-template>
          </xsl:for-each>

        </table>

      </body>
    </html>
  </xsl:template>

  <xsl:template name="GroupResult">
    <xsl:param name="groupElt"/>
    <xsl:param name="parentsTitle"/>

    <!-- construct group title -->
    <xsl:variable name="groupTitle">
      <xsl:choose>
        <xsl:when test="$parentsTitle != ''"><xsl:value-of select="concat($parentsTitle, ' &#187; ', ./xccdf:title/text())"/></xsl:when>
        <xsl:otherwise><xsl:value-of select="./xccdf:title/text()"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- if group has child rules, output title/rules -->
    <xsl:if test="$groupElt/xccdf:Rule">
      <tr class="group"><td colspan="3"><xsl:value-of select="$groupTitle"/></td></tr>

      <xsl:for-each select="$groupElt/xccdf:Rule">
        <xsl:call-template name="RuleResult">
          <xsl:with-param name="ruleElt" select="."/>
        </xsl:call-template>
      </xsl:for-each>
    </xsl:if>

    <!-- output child groups -->
    <xsl:for-each select="$groupElt/xccdf:Group">
      <xsl:call-template name="GroupResult">
        <xsl:with-param name="groupElt" select="."/>
        <xsl:with-param name="parentsTitle" select="$groupTitle"/>
      </xsl:call-template>
    </xsl:for-each>    
  </xsl:template>


  <xsl:template name="RuleResult">
    <xsl:param name="ruleElt"/>
    <xsl:variable name="ruleId" select="$ruleElt/@id"/>
    <xsl:variable name="ruleResultElt" select="/xccdf:Benchmark/xccdf:TestResult/xccdf:rule-result[@idref = $ruleId]"/>
    <xsl:variable name="ruleResult" select="$ruleResultElt/xccdf:result/text()"/>
    <xsl:variable name="ruleResultId" select="translate($ruleResult, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ ','abcdefghijklmnopqrstuvwxyz_')"/>
    <xsl:variable name="ruleDiagnosticsElt" select="$ruleResultElt/xccdf:metadata/diagnostic:rule_diagnostics"/>
    <xsl:variable name="ruleDiagnosticsChildren" select="$ruleDiagnosticsElt/*"/>

    <tr>
      <xsl:attribute name="class">rule <xsl:value-of select="$ruleResultId" /></xsl:attribute>

      <td class="rule-title">
        <xsl:choose>
          <xsl:when test="$ruleDiagnosticsChildren">
            <a href="#" class="toggleDiagnostics"><xsl:value-of select="$ruleElt/xccdf:title/text()"/></a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$ruleElt/xccdf:title/text()"/>
          </xsl:otherwise>
        </xsl:choose>
      </td>

      <td class="identifiers">
        <xsl:for-each select="$ruleElt/xccdf:ident">
          <xsl:call-template name="RuleIdentifier">
            <xsl:with-param name="identElt" select="."/>
          </xsl:call-template>
          <xsl:if test="position() != last()">, </xsl:if>
        </xsl:for-each>
      </td>

      <td>
        <xsl:attribute name="class">rule-result result-<xsl:value-of select="$ruleResultId" /></xsl:attribute>
        <xsl:value-of select="$ruleResult" />
      </td>
    </tr>

    <xsl:if test="$ruleDiagnosticsChildren">
      <tr class="diagnostics">
        <td colspan="3"><div class="diagnostics-content">

          <h3><xsl:value-of select="$ruleElt/xccdf:title/text()"/> Result Diagnostics</h3>
          <div class="rule-description"><xsl:value-of select="$ruleElt/xccdf:description/text()"/></div>

          <div class="xmlRuleDiagnostics">
            <xsl:attribute name="result"><xsl:value-of select="$ruleResultId" /></xsl:attribute>

            <xsl:call-template name="RemoveNS">
              <xsl:with-param name="sourceElt" select="$ruleResultElt/xccdf:complex-check"/>
            </xsl:call-template>
            
            <xsl:call-template name="RemoveNS">
              <xsl:with-param name="sourceElt" select="$ruleDiagnosticsElt"/>
            </xsl:call-template>
          </div>

        </div></td>
      </tr>
    </xsl:if>
          
  </xsl:template>

  <xsl:template name="RuleIdentifier">
    <xsl:param name="identElt"/>
    <xsl:variable name="system" select="$identElt/@system"/>
    <xsl:variable name="identifier" select="$identElt/text()"/>

    <xsl:choose>

      <xsl:when test="./@system = 'http://cce.mitre.org'">
        <a class="cce" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://scapsync.com/cce/', $identifier)" /></xsl:attribute>
          <!-- alternate source: http://www.scaprepo.com/control.jsp?command=search&search=$identifier -->
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = 'http://cpe.mitre.org'">
        <a class="cpe" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://web.nvd.nist.gov/view/cpe/search/results?searchChoice=name&amp;includeDeprecated=on&amp;searchText=', $identifier)" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = 'http://cve.mitre.org'">
        <a class="cve" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://web.nvd.nist.gov/view/vuln/detail?vulnId=', $identifier)" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = 'http://www.cert.org'">
        <a class="cert-advisory" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://www.cert.org/advisories/', $identifier, '.html')" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = 'http://www.kb.cert.org'">
        <a class="cert-vulnerability" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://www.kb.cert.org/vuls/id/', $identifier)" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = 'http://www.us-cert.gov/cas/techalerts'">
        <a class="cert-alert" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="concat('http://www.us-cert.gov/ncas/alerts/', $identifier)" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:when>

      <xsl:when test="./@system = ''">
        <span class="ident-no-system">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </span>
      </xsl:when>

      <xsl:otherwise>
        <a class="ident-other-system" target="_blank">
          <xsl:attribute name="rel"><xsl:value-of select="$identifier" /></xsl:attribute>
          <xsl:attribute name="href"><xsl:value-of select="$system" /></xsl:attribute>
          <xsl:value-of select="$identifier" />
        </a>
      </xsl:otherwise>

    </xsl:choose>

  </xsl:template>

  <xsl:template name="DetailJsCss">
    <script langauge="javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js"></script>
    <script langauge="javascript">
        <![CDATA[ 
          /* highlight.js */
          var hljs=new function(){function l(o){return o.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;")}function b(p){for(var o=p.firstChild;o;o=o.nextSibling){if(o.nodeName=="CODE"){return o}if(!(o.nodeType==3&&o.nodeValue.match(/\s+/))){break}}}function h(p,o){return Array.prototype.map.call(p.childNodes,function(q){if(q.nodeType==3){return o?q.nodeValue.replace(/\n/g,""):q.nodeValue}if(q.nodeName=="BR"){return"\n"}return h(q,o)}).join("")}function a(q){var p=(q.className+" "+q.parentNode.className).split(/\s+/);p=p.map(function(r){return r.replace(/^language-/,"")});for(var o=0;o<p.length;o++){if(e[p[o]]||p[o]=="no-highlight"){return p[o]}}}function c(q){var o=[];(function p(r,s){for(var t=r.firstChild;t;t=t.nextSibling){if(t.nodeType==3){s+=t.nodeValue.length}else{if(t.nodeName=="BR"){s+=1}else{if(t.nodeType==1){o.push({event:"start",offset:s,node:t});s=p(t,s);o.push({event:"stop",offset:s,node:t})}}}}return s})(q,0);return o}function j(x,v,w){var p=0;var y="";var r=[];function t(){if(x.length&&v.length){if(x[0].offset!=v[0].offset){return(x[0].offset<v[0].offset)?x:v}else{return v[0].event=="start"?x:v}}else{return x.length?x:v}}function s(A){function z(B){return" "+B.nodeName+'="'+l(B.value)+'"'}return"<"+A.nodeName+Array.prototype.map.call(A.attributes,z).join("")+">"}while(x.length||v.length){var u=t().splice(0,1)[0];y+=l(w.substr(p,u.offset-p));p=u.offset;if(u.event=="start"){y+=s(u.node);r.push(u.node)}else{if(u.event=="stop"){var o,q=r.length;do{q--;o=r[q];y+=("</"+o.nodeName.toLowerCase()+">")}while(o!=u.node);r.splice(q,1);while(q<r.length){y+=s(r[q]);q++}}}}return y+l(w.substr(p))}function f(q){function o(s,r){return RegExp(s,"m"+(q.cI?"i":"")+(r?"g":""))}function p(y,w){if(y.compiled){return}y.compiled=true;var s=[];if(y.k){var r={};function z(A,t){t.split(" ").forEach(function(B){var C=B.split("|");r[C[0]]=[A,C[1]?Number(C[1]):1];s.push(C[0])})}y.lR=o(y.l||hljs.IR,true);if(typeof y.k=="string"){z("keyword",y.k)}else{for(var x in y.k){if(!y.k.hasOwnProperty(x)){continue}z(x,y.k[x])}}y.k=r}if(w){if(y.bWK){y.b="\\b("+s.join("|")+")\\s"}y.bR=o(y.b?y.b:"\\B|\\b");if(!y.e&&!y.eW){y.e="\\B|\\b"}if(y.e){y.eR=o(y.e)}y.tE=y.e||"";if(y.eW&&w.tE){y.tE+=(y.e?"|":"")+w.tE}}if(y.i){y.iR=o(y.i)}if(y.r===undefined){y.r=1}if(!y.c){y.c=[]}for(var v=0;v<y.c.length;v++){if(y.c[v]=="self"){y.c[v]=y}p(y.c[v],y)}if(y.starts){p(y.starts,w)}var u=[];for(var v=0;v<y.c.length;v++){u.push(y.c[v].b)}if(y.tE){u.push(y.tE)}if(y.i){u.push(y.i)}y.t=u.length?o(u.join("|"),true):{exec:function(t){return null}}}p(q)}function d(D,E){function o(r,M){for(var L=0;L<M.c.length;L++){var K=M.c[L].bR.exec(r);if(K&&K.index==0){return M.c[L]}}}function s(K,r){if(K.e&&K.eR.test(r)){return K}if(K.eW){return s(K.parent,r)}}function t(r,K){return K.i&&K.iR.test(r)}function y(L,r){var K=F.cI?r[0].toLowerCase():r[0];return L.k.hasOwnProperty(K)&&L.k[K]}function G(){var K=l(w);if(!A.k){return K}var r="";var N=0;A.lR.lastIndex=0;var L=A.lR.exec(K);while(L){r+=K.substr(N,L.index-N);var M=y(A,L);if(M){v+=M[1];r+='<span class="'+M[0]+'">'+L[0]+"</span>"}else{r+=L[0]}N=A.lR.lastIndex;L=A.lR.exec(K)}return r+K.substr(N)}function z(){if(A.sL&&!e[A.sL]){return l(w)}var r=A.sL?d(A.sL,w):g(w);if(A.r>0){v+=r.keyword_count;B+=r.r}return'<span class="'+r.language+'">'+r.value+"</span>"}function J(){return A.sL!==undefined?z():G()}function I(L,r){var K=L.cN?'<span class="'+L.cN+'">':"";if(L.rB){x+=K;w=""}else{if(L.eB){x+=l(r)+K;w=""}else{x+=K;w=r}}A=Object.create(L,{parent:{value:A}});B+=L.r}function C(K,r){w+=K;if(r===undefined){x+=J();return 0}var L=o(r,A);if(L){x+=J();I(L,r);return L.rB?0:r.length}var M=s(A,r);if(M){if(!(M.rE||M.eE)){w+=r}x+=J();do{if(A.cN){x+="</span>"}A=A.parent}while(A!=M.parent);if(M.eE){x+=l(r)}w="";if(M.starts){I(M.starts,"")}return M.rE?0:r.length}if(t(r,A)){throw"Illegal"}w+=r;return r.length||1}var F=e[D];f(F);var A=F;var w="";var B=0;var v=0;var x="";try{var u,q,p=0;while(true){A.t.lastIndex=p;u=A.t.exec(E);if(!u){break}q=C(E.substr(p,u.index-p),u[0]);p=u.index+q}C(E.substr(p));return{r:B,keyword_count:v,value:x,language:D}}catch(H){if(H=="Illegal"){return{r:0,keyword_count:0,value:l(E)}}else{throw H}}}function g(s){var o={keyword_count:0,r:0,value:l(s)};var q=o;for(var p in e){if(!e.hasOwnProperty(p)){continue}var r=d(p,s);r.language=p;if(r.keyword_count+r.r>q.keyword_count+q.r){q=r}if(r.keyword_count+r.r>o.keyword_count+o.r){q=o;o=r}}if(q.language){o.second_best=q}return o}function i(q,p,o){if(p){q=q.replace(/^((<[^>]+>|\t)+)/gm,function(r,v,u,t){return v.replace(/\t/g,p)})}if(o){q=q.replace(/\n/g,"<br>")}return q}function m(r,u,p){var v=h(r,p);var t=a(r);if(t=="no-highlight"){return}var w=t?d(t,v):g(v);t=w.language;var o=c(r);if(o.length){var q=document.createElement("pre");q.innerHTML=w.value;w.value=j(o,c(q),v)}w.value=i(w.value,u,p);var s=r.className;if(!s.match("(\\s|^)(language-)?"+t+"(\\s|$)")){s=s?(s+" "+t):t}r.innerHTML=w.value;r.className=s;r.result={language:t,kw:w.keyword_count,re:w.r};if(w.second_best){r.second_best={language:w.second_best.language,kw:w.second_best.keyword_count,re:w.second_best.r}}}function n(){if(n.called){return}n.called=true;Array.prototype.map.call(document.getElementsByTagName("pre"),b).filter(Boolean).forEach(function(o){m(o,hljs.tabReplace)})}function k(){window.addEventListener("DOMContentLoaded",n,false);window.addEventListener("load",n,false)}var e={};this.LANGUAGES=e;this.highlight=d;this.highlightAuto=g;this.fixMarkup=i;this.highlightBlock=m;this.initHighlighting=n;this.initHighlightingOnLoad=k;this.IR="[a-zA-Z][a-zA-Z0-9_]*";this.UIR="[a-zA-Z_][a-zA-Z0-9_]*";this.NR="\\b\\d+(\\.\\d+)?";this.CNR="(\\b0[xX][a-fA-F0-9]+|(\\b\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?)";this.BNR="\\b(0b[01]+)";this.RSR="!|!=|!==|%|%=|&|&&|&=|\\*|\\*=|\\+|\\+=|,|\\.|-|-=|/|/=|:|;|<|<<|<<=|<=|=|==|===|>|>=|>>|>>=|>>>|>>>=|\\?|\\[|\\{|\\(|\\^|\\^=|\\||\\|=|\\|\\||~";this.BE={b:"\\\\[\\s\\S]",r:0};this.ASM={cN:"string",b:"'",e:"'",i:"\\n",c:[this.BE],r:0};this.QSM={cN:"string",b:'"',e:'"',i:"\\n",c:[this.BE],r:0};this.CLCM={cN:"comment",b:"//",e:"$"};this.CBLCLM={cN:"comment",b:"/\\*",e:"\\*/"};this.HCM={cN:"comment",b:"#",e:"$"};this.NM={cN:"number",b:this.NR,r:0};this.CNM={cN:"number",b:this.CNR,r:0};this.BNM={cN:"number",b:this.BNR,r:0};this.inherit=function(q,r){var o={};for(var p in q){o[p]=q[p]}if(r){for(var p in r){o[p]=r[p]}}return o}}();hljs.LANGUAGES.bash=function(a){var g="true false";var e="if then else elif fi for break continue while in do done echo exit return set declare";var c={cN:"variable",b:"\\$[a-zA-Z0-9_#]+"};var b={cN:"variable",b:"\\${([^}]|\\\\})+}"};var h={cN:"string",b:'"',e:'"',i:"\\n",c:[a.BE,c,b],r:0};var d={cN:"string",b:"'",e:"'",c:[{b:"''"}],r:0};var f={cN:"test_condition",b:"",e:"",c:[h,d,c,b],k:{literal:g},r:0};return{k:{keyword:e,literal:g},c:[{cN:"shebang",b:"(#!\\/bin\\/bash)|(#!\\/bin\\/sh)",r:10},c,b,a.HCM,h,d,a.inherit(f,{b:"\\[ ",e:" \\]",r:0}),a.inherit(f,{b:"\\[\\[ ",e:" \\]\\]"})]}}(hljs);hljs.LANGUAGES.javascript=function(a){return{k:{keyword:"in if for while finally var new function do return void else break catch instanceof with throw case default try this switch continue typeof delete let yield const",literal:"true false null undefined NaN Infinity"},c:[a.ASM,a.QSM,a.CLCM,a.CBLCLM,a.CNM,{b:"("+a.RSR+"|\\b(case|return|throw)\\b)\\s*",k:"return throw case",c:[a.CLCM,a.CBLCLM,{cN:"regexp",b:"/",e:"/[gim]*",i:"\\n",c:[{b:"\\\\/"}]},{b:"<",e:">;",sL:"xml"}],r:0},{cN:"function",bWK:true,e:"{",k:"function",c:[{cN:"title",b:"[A-Za-z$_][0-9A-Za-z$_]*"},{cN:"params",b:"\\(",e:"\\)",c:[a.CLCM,a.CBLCLM],i:"[\"'\\(]"}],i:"\\[|%"}]}}(hljs);hljs.LANGUAGES.xml=function(a){var c="[A-Za-z0-9\\._:-]+";var b={eW:true,c:[{cN:"attribute",b:c,r:0},{b:'="',rB:true,e:'"',c:[{cN:"value",b:'"',eW:true}]},{b:"='",rB:true,e:"'",c:[{cN:"value",b:"'",eW:true}]},{b:"=",c:[{cN:"value",b:"[^\\s/>]+"}]}]};return{cI:true,c:[{cN:"pi",b:"<\\?",e:"\\?>",r:10},{cN:"doctype",b:"<!DOCTYPE",e:">",r:10,c:[{b:"\\[",e:"\\]"}]},{cN:"comment",b:"<!--",e:"-->",r:10},{cN:"cdata",b:"<\\!\\[CDATA\\[",e:"\\]\\]>",r:10},{cN:"tag",b:"<style(?=\\s|>|$)",e:">",k:{title:"style"},c:[b],starts:{e:"</style>",rE:true,sL:"css"}},{cN:"tag",b:"<script(?=\\s|>|$)",e:">",k:{title:"script"},c:[b],starts:{e:"<\/script>",rE:true,sL:"javascript"}},{b:"<%",e:"%>",sL:"vbscript"},{cN:"tag",b:"</?",e:"/?>",c:[{cN:"title",b:"[^ />]+"},b]}]}}(hljs);hljs.LANGUAGES.java=function(a){return{k:"false synchronized int abstract float private char boolean static null if const for true while long throw strictfp finally protected import native final return void enum else break transient new catch instanceof byte super volatile case assert short package default double public try this switch continue throws",c:[{cN:"javadoc",b:"/\\*\\*",e:"\\*/",c:[{cN:"javadoctag",b:"@[A-Za-z]+"}],r:10},a.CLCM,a.CBLCLM,a.ASM,a.QSM,{cN:"class",bWK:true,e:"{",k:"class interface",i:":",c:[{bWK:true,k:"extends implements",r:10},{cN:"title",b:a.UIR}]},a.CNM,{cN:"annotation",b:"@[A-Za-z]+"}]}}(hljs);hljs.LANGUAGES.python=function(a){var f={cN:"prompt",b:"^(>>>|\\.\\.\\.) "};var c=[{cN:"string",b:"(u|b)?r?'''",e:"'''",c:[f],r:10},{cN:"string",b:'(u|b)?r?"""',e:'"""',c:[f],r:10},{cN:"string",b:"(u|r|ur)'",e:"'",c:[a.BE],r:10},{cN:"string",b:'(u|r|ur)"',e:'"',c:[a.BE],r:10},{cN:"string",b:"(b|br)'",e:"'",c:[a.BE]},{cN:"string",b:'(b|br)"',e:'"',c:[a.BE]}].concat([a.ASM,a.QSM]);var e={cN:"title",b:a.UIR};var d={cN:"params",b:"\\(",e:"\\)",c:["self",a.CNM,f].concat(c)};var b={bWK:true,e:":",i:"[${=;\\n]",c:[e,d],r:10};return{k:{keyword:"and elif is global as in if from raise for except finally print import pass return exec else break not with class assert yield try while continue del or def lambda nonlocal|10",built_in:"None True False Ellipsis NotImplemented"},i:"(</|->|\\?)",c:c.concat([f,a.HCM,a.inherit(b,{cN:"function",k:"def"}),a.inherit(b,{cN:"class",k:"class"}),a.CNM,{cN:"decorator",b:"@",e:"$"},{b:"\\b(print|exec)\\("}])}}(hljs);hljs.LANGUAGES.actionscript=function(a){var d="[a-zA-Z_$][a-zA-Z0-9_$]*";var c="([*]|[a-zA-Z_$][a-zA-Z0-9_$]*)";var e={cN:"rest_arg",b:"[.]{3}",e:d,r:10};var b={cN:"title",b:d};return{k:{keyword:"as break case catch class const continue default delete do dynamic each else extends final finally for function get if implements import in include instanceof interface internal is namespace native new override package private protected public return set static super switch this throw try typeof use var void while with",literal:"true false null undefined"},c:[a.ASM,a.QSM,a.CLCM,a.CBLCLM,a.CNM,{cN:"package",bWK:true,e:"{",k:"package",c:[b]},{cN:"class",bWK:true,e:"{",k:"class interface",c:[{bWK:true,k:"extends implements"},b]},{cN:"preprocessor",bWK:true,e:";",k:"import include"},{cN:"function",bWK:true,e:"[{;]",k:"function",i:"\\S",c:[b,{cN:"params",b:"\\(",e:"\\)",c:[a.ASM,a.QSM,a.CLCM,a.CBLCLM,e]},{cN:"type",b:":",e:c,r:10}]}]}}(hljs);hljs.LANGUAGES.ini=function(a){return{cI:true,i:"[^\\s]",c:[{cN:"comment",b:";",e:"$"},{cN:"title",b:"^\\[",e:"\\]"},{cN:"setting",b:"^[a-z0-9\\[\\]_-]+[ \\t]*=[ \\t]*",e:"$",c:[{cN:"value",eW:true,k:"on off true false yes no",c:[a.QSM,a.NM]}]}]}}(hljs);hljs.LANGUAGES.perl=function(e){var a="getpwent getservent quotemeta msgrcv scalar kill dbmclose undef lc ma syswrite tr send umask sysopen shmwrite vec qx utime local oct semctl localtime readpipe do return format read sprintf dbmopen pop getpgrp not getpwnam rewinddir qqfileno qw endprotoent wait sethostent bless s|0 opendir continue each sleep endgrent shutdown dump chomp connect getsockname die socketpair close flock exists index shmgetsub for endpwent redo lstat msgctl setpgrp abs exit select print ref gethostbyaddr unshift fcntl syscall goto getnetbyaddr join gmtime symlink semget splice x|0 getpeername recv log setsockopt cos last reverse gethostbyname getgrnam study formline endhostent times chop length gethostent getnetent pack getprotoent getservbyname rand mkdir pos chmod y|0 substr endnetent printf next open msgsnd readdir use unlink getsockopt getpriority rindex wantarray hex system getservbyport endservent int chr untie rmdir prototype tell listen fork shmread ucfirst setprotoent else sysseek link getgrgid shmctl waitpid unpack getnetbyname reset chdir grep split require caller lcfirst until warn while values shift telldir getpwuid my getprotobynumber delete and sort uc defined srand accept package seekdir getprotobyname semop our rename seek if q|0 chroot sysread setpwent no crypt getc chown sqrt write setnetent setpriority foreach tie sin msgget map stat getlogin unless elsif truncate exec keys glob tied closedirioctl socket readlink eval xor readline binmode setservent eof ord bind alarm pipe atan2 getgrent exp time push setgrent gt lt or ne m|0 break given say state when";var d={cN:"subst",b:"[$@]\\{",e:"\\}",k:a,r:10};var b={cN:"variable",b:"\\$\\d"};var i={cN:"variable",b:"[\\$\\%\\@\\*](\\^\\w\\b|#\\w+(\\:\\:\\w+)*|[^\\s\\w{]|{\\w+}|\\w+(\\:\\:\\w*)*)"};var f=[e.BE,d,b,i];var h={b:"->",c:[{b:e.IR},{b:"{",e:"}"}]};var g={cN:"comment",b:"^(__END__|__DATA__)",e:"\\n$",r:5};var c=[b,i,e.HCM,g,{cN:"comment",b:"^\\=\\w",e:"\\=cut",eW:true},h,{cN:"string",b:"q[qwxr]?\\s*\\(",e:"\\)",c:f,r:5},{cN:"string",b:"q[qwxr]?\\s*\\[",e:"\\]",c:f,r:5},{cN:"string",b:"q[qwxr]?\\s*\\{",e:"\\}",c:f,r:5},{cN:"string",b:"q[qwxr]?\\s*\\|",e:"\\|",c:f,r:5},{cN:"string",b:"q[qwxr]?\\s*\\<",e:"\\>",c:f,r:5},{cN:"string",b:"qw\\s+q",e:"q",c:f,r:5},{cN:"string",b:"'",e:"'",c:[e.BE],r:0},{cN:"string",b:'"',e:'"',c:f,r:0},{cN:"string",b:"`",e:"`",c:[e.BE]},{cN:"string",b:"{\\w+}",r:0},{cN:"string",b:"-?\\w+\\s*\\=\\>",r:0},{cN:"number",b:"(\\b0[0-7_]+)|(\\b0x[0-9a-fA-F_]+)|(\\b[1-9][0-9_]*(\\.[0-9_]+)?)|[0_]\\b",r:0},{b:"("+e.RSR+"|\\b(split|return|print|reverse|grep)\\b)\\s*",k:"split return print reverse grep",r:0,c:[e.HCM,g,{cN:"regexp",b:"(s|tr|y)/(\\\\.|[^/])*/(\\\\.|[^/])*/[a-z]*",r:10},{cN:"regexp",b:"(m|qr)?/",e:"/[a-z]*",c:[e.BE],r:0}]},{cN:"sub",bWK:true,e:"(\\s*\\(.*?\\))?[;{]",k:"sub",r:5},{cN:"operator",b:"-\\w\\b",r:0}];d.c=c;h.c[1].c=c;return{k:a,c:c}}(hljs);hljs.LANGUAGES.vbscript=function(a){return{cI:true,k:{keyword:"call class const dim do loop erase execute executeglobal exit for each next function if then else on error option explicit new private property let get public randomize redim rem select case set stop sub while wend with end to elseif is or xor and not class_initialize class_terminate default preserve in me byval byref step resume goto",built_in:"lcase month vartype instrrev ubound setlocale getobject rgb getref string weekdayname rnd dateadd monthname now day minute isarray cbool round formatcurrency conversions csng timevalue second year space abs clng timeserial fixs len asc isempty maths dateserial atn timer isobject filter weekday datevalue ccur isdate instr datediff formatdatetime replace isnull right sgn array snumeric log cdbl hex chr lbound msgbox ucase getlocale cos cdate cbyte rtrim join hour oct typename trim strcomp int createobject loadpicture tan formatnumber mid scriptenginebuildversion scriptengine split scriptengineminorversion cint sin datepart ltrim sqr scriptenginemajorversion time derived eval date formatpercent exp inputbox left ascw chrw regexp server response request cstr err",literal:"true false null nothing empty"},i:"//",c:[a.inherit(a.QSM,{c:[{b:'""'}]}),{cN:"comment",b:"'",e:"$"},a.CNM]}}(hljs);hljs.LANGUAGES.dos=function(a){return{cI:true,k:{flow:"if else goto for in do call exit not exist errorlevel defined equ neq lss leq gtr geq",keyword:"shift cd dir echo setlocal endlocal set pause copy",stream:"prn nul lpt3 lpt2 lpt1 con com4 com3 com2 com1 aux",winutils:"ping net ipconfig taskkill xcopy ren del"},c:[{cN:"envvar",b:"%%[^ ]"},{cN:"envvar",b:"%[^ ]+?%"},{cN:"envvar",b:"![^ ]+?!"},{cN:"number",b:"\\b\\d+",r:0},{cN:"comment",b:"@?rem",e:"$"}]}}(hljs);hljs.LANGUAGES.applescript=function(a){var b=a.inherit(a.QSM,{i:""});var e={cN:"title",b:a.UIR};var d={cN:"params",b:"\\(",e:"\\)",c:["self",a.CNM,b]};var c=[{cN:"comment",b:"--",e:"$",},{cN:"comment",b:"\\(\\*",e:"\\*\\)",c:["self",{b:"--",e:"$"}]},a.HCM];return{k:{keyword:"about above after against and around as at back before beginning behind below beneath beside between but by considering contain contains continue copy div does eighth else end equal equals error every exit fifth first for fourth from front get given global if ignoring in into is it its last local me middle mod my ninth not of on onto or over prop property put ref reference repeat returning script second set seventh since sixth some tell tenth that the then third through thru timeout times to transaction try until where while whose with without",constant:"AppleScript false linefeed return pi quote result space tab true",type:"alias application boolean class constant date file integer list number real record string text",command:"activate beep count delay launch log offset read round run say summarize write",property:"character characters contents day frontmost id item length month name paragraph paragraphs rest reverse running time version weekday word words year"},c:[b,a.CNM,{cN:"type",b:"\\bPOSIX file\\b"},{cN:"command",b:"\\b(clipboard info|the clipboard|info for|list (disks|folder)|mount volume|path to|(close|open for) access|(get|set) eof|current date|do shell script|get volume settings|random number|set volume|system attribute|system info|time to GMT|(load|run|store) script|scripting components|ASCII (character|number)|localized string|choose (application|color|file|file name|folder|from list|remote application|URL)|display (alert|dialog))\\b|^\\s*return\\b"},{cN:"constant",b:"\\b(text item delimiters|current application|missing value)\\b"},{cN:"keyword",b:"\\b(apart from|aside from|instead of|out of|greater than|isn't|(doesn't|does not) (equal|come before|come after|contain)|(greater|less) than( or equal)?|(starts?|ends|begins?) with|contained by|comes (before|after)|a (ref|reference))\\b"},{cN:"property",b:"\\b(POSIX path|(date|time) string|quoted form)\\b"},{cN:"function_start",bWK:true,k:"on",i:"[${=;\\n]",c:[e,d]}].concat(c)}}(hljs);
        ]]>
    </script>
    <style type="text/css">
      pre code{display:block;background:#FFF;color:#000;padding:.5em}
      pre .comment,pre .template_comment,pre .javadoc,pre .comment *{color:#800}
      pre .keyword,pre .method,pre .list .title,pre .clojure .built_in,pre .nginx .title,pre .tag .title,pre .setting .value,pre .winutils,pre .tex .command,pre .http .title,pre .request,pre .status{color:#008}
      pre .envvar,pre .tex .special{color:#660}
      pre .string,pre .tag .value,pre .cdata,pre .filter .argument,pre .attr_selector,pre .apache .cbracket,pre .date,pre .regexp,pre .coffeescript .attribute{color:#080}
      pre .sub .identifier,pre .pi,pre .tag,pre .tag .keyword,pre .decorator,pre .ini .title,pre .shebang,pre .prompt,pre .hexcolor,pre .rules .value,pre .css .value .number,pre .literal,pre .symbol,pre .ruby .symbol .string,pre .number,pre .css .function,pre .clojure .attribute{color:#066}
      pre .class .title,pre .haskell .type,pre .smalltalk .class,pre .javadoctag,pre .yardoctag,pre .phpdoc,pre .typename,pre .tag .attribute,pre .doctype,pre .class .id,pre .built_in,pre .setting,pre .params,pre .variable,pre .clojure .title{color:#606}
      pre .css .tag,pre .rules .property,pre .pseudo,pre .subst{color:#000}
      pre .css .class,pre .css .id{color:#9B703F}
      pre .value .important{color:#f70;font-weight:700}
      pre .rules .keyword{color:#C5AF75}
      pre .annotation,pre .apache .sqbracket,pre .nginx .built_in{color:#9B859D}
      pre .preprocessor,pre .preprocessor *{color:#444}
      pre .tex .formula{background-color:#EEE;font-style:italic}
      pre .diff .header,pre .chunk{color:gray;font-weight:700}
      pre .diff .change{background-color:#BCCFF9}
      pre .addition{background-color:#BAEEBA}
      pre .deletion{background-color:#FFC8BD}
      pre .comment .yardoctag{font-weight:700}
    </style>
    <style type="text/css">
      /* http://meyerweb.com/eric/tools/css/reset/ v2.0 | 20110126  License: none (public domain) */
      html,body,div,span,applet,object,iframe,h1,h2,h3,h4,h5,h6,p,blockquote,pre,a,abbr,acronym,address,big,cite,code,del,dfn,em,img,ins,kbd,q,s,samp,small,strike,strong,sub,sup,tt,var,b,u,i,center,dl,dt,dd,ol,ul,li,fieldset,form,label,legend,table,caption,tbody,tfoot,thead,tr,th,td,article,aside,canvas,details,embed,figure,figcaption,footer,header,hgroup,menu,nav,output,ruby,section,summary,time,mark,audio,video{border:0;font-size:100%;font:inherit;vertical-align:baseline;margin:0;padding:0}article,aside,details,figcaption,figure,footer,header,hgroup,menu,nav,section{display:block}body{line-height:1}ol,ul{list-style:none}blockquote,q{quotes:none}blockquote:before,blockquote:after,q:before,q:after{content:none}table{border-collapse:collapse;border-spacing:0}


      /* Document Defaults */

      body { font:normal 13px/18px Tahoma,'Lucida Grande',Verdana,Arial,Helvetica,sans-serif; color:#333; background-color:#fff; padding:20px; }
        small { font-size:0.80em; padding-left:5px; font-weight:normal; opacity:0.7; filter:alpha(opacity=70); }
          small:hover { opacity:1; filter:alpha(opacity=100); }

      a, a:link, a:visited { color:#327dcd; text-decoration:none; }
        a:active, a:hover { color:#0064cd; text-decoration:underline; }

      h1, h2, h3, h4, h5, h6  { margin:0 0 9px 0; text-rendering:optimizelegibility; line-height:1.1em; font-weight:bold; }
      h1 { font-size:32px; font-weight:normal; }
      h2 { font-size:26px; font-weight:normal; }
      h3 { font-size:21px; font-weight:normal; }
      h4 { font-size:18px; font-weight:normal; }
      h5 { font-size:16px; font-weight:normal; }
      h6 { font-size:13px; }

      table { width:100%; margin-bottom:18px; }
        td { padding:4px 5px; line-height:18px; text-align:left; vertical-align:top; border:1px solid #bbb; }

        tr.header {}
        tr.header td { font-weight:bold; vertical-align:bottom; background:#d3d3d3; }

        table.striped tr:nth-child(odd) td, .altBg { background-color:#f9f9f9; }
          table.striped tr.header:nth-child(odd) td { background:#d3d3d3; }

      pre {  white-space:pre-wrap; white-space:-moz-pre-wrap; white-space:-pre-wrap; white-space:-o-pre-wrap; word-wrap:break-word; overflow-x:auto; }
        pre code { background-color:#eee; padding:9px; font-family:Consolas,"Liberation Mono",Courier,monospace; font-size:12px; line-height:1.1em; } 

      /* Section-Specific Styles */

      table.keyvalue {}
        table.keyvalue td:first-child { font-weight:bold; }
        table.keyvalue td:last-child {}

      table.scores { }
        table.scores td.method { text-align:left; }
        table.scores td.score { text-align:right; width:100px; }
        table.scores td.maximum { text-align:right; width:100px; }
        table.scores td.percentage { text-align:right; width:100px; }

      table.rule-results {}
        table.rule-results tr.group { background:#e3e3e3; font-weight:bold; }
        table.rule-results tr.rule {}
          table.rule-results td.rule-title {}
          table.rule-results td.identifiers { width:212px; text-align:center; }
          table.rule-results td.rule-result { width:100px; text-align:center; text-transform:uppercase; }
            table.rule-results tr.header td.rule-result { text-transform:none; }

      /* Diagnostics Section Styles */

      a.toggleDiagnostics { }

      table.rule-results tr.diagnostics { display:none; background-color:#f3f3f3; }
        table.rule-results tr.diagnostics > td { padding:8px; }
          div.diagnostics-content {display:none; }
            div.xmlRuleDiagnostics { display:none; }
            div.rule-description { margin:0 0 18px 0; }
            div.divDiagnosticsView { text-align:left; }

      ul.logic { margin:0 0 18px 0; padding:5px 18px 9px 0; font-size:14px; }
        ul.logic, ul.logic ul { list-style:none; }
        ul.logic ul { margin:0 0 0 18px; padding:5px 0 5px 0; }
        ul.logic li { padding: 3px 0 3px 9px; margin: 0px 0;}
        li.group span.operator { text-transform:uppercase; font-weight:bold; }
        li.group span.modifier { font-size:0.8em; }
          li.group.result-true > ul { border-left:2px solid #468847; }
          li.group.result-false > ul { border-left:2px solid #b94a48; }
          li.group.result-unknown > ul { border-left:2px solid #ccc; }
          li.group.result-error > ul { border-left:2px solid #ccc; }
          li.group.result-not_evaluated > ul { border-left:2px solid #ccc; }
          li.group.result-not_applicable > ul { border-left:2px solid #ccc; }

        li.extend-definition.group { text-transform:none; font-weight:normal; }

        li.test, li.sce { }
        span.test-title,
        span.sce-title { cursor:pointer; text-transform:none; font-weight:normal; }

        div.detail-section { background-color:#fff; color:#333; margin-bottom:18px; }
          div.detail-section.result-true, div.detail-section.result-pass, ul.logic.result-true { border:1px solid #468847; }
          div.detail-section.result-false, div.detail-section.result-fail, ul.logic.result-fail { border:1px solid #b94a48; }
          div.detail-section.result-informational, ul.logic.result-informational { border:1px solid #3a87ad; }
          div.detail-section.result-unknown, ul.logic.result-unknown { border:1px solid #999; }
          div.detail-section.result-error, ul.logic.result-error { border:1px solid #c09853; }
          div.detail-section.result-notevaluated,  ul.logic.result-notevaluated { border:1px solid #999; }
          div.detail-section.result-notapplicable, ul.logic.result-notapplicable { border:1px solid #999; }

          div.detail-section h5 { font-size:14px; padding:5px 9px; }
          div.detail-section > div, 
          div.detail-section > table,
          div.detail-section > pre,
          div.detail-section > h6 { margin:0 9px 9px 9px;}

          table.value-detail { width:auto; margin-bottom:18px; }
            table.value-detail td { padding:4px 5px; font-size:12px; line-height:1.1em; }
            table.value-detail tr.reference td { padding:2px 5px; font-size:10px; line-height:1.1em; background-color:#f3f3f3; color:#666; }
              table.value-detail tr.reference.hidden { display:none; }
            table.value-detail tr.reference-toggle td { padding:2px 5px; font-size:10px; line-height:1.1em; background-color:#fff; border:0; }

          div.processing-error { color:#c09853; background-color:#fcf8e3; border:1px solid #c09853; margin-bottom:9px; padding:3px 9px;  }
          div.existence-stateop {}
          div.obj-coll-issue {}
        
        .result-true, .result-pass { color:#468847; background-color:#dff0d8; }
        .result-false, .result-fail { color:#b94a48; background-color:#f2dede; }
        .result-informational { color:#3a87ad; background-color:#d9edf7; }
        .result-unknown { color:#999; background-color:#ddd; }
        .result-error, .result-notchecked { color:#c09853; background-color:#fcf8e3; }
        .result-notevaluated { color:#999; background-color:#ddd; }
        .result-notselected { color:#999; background-color:#ddd; }
        .result-notapplicable { color:#999; background-color:#ddd; }
    </style>
    <script langauge="javascript">
        <![CDATA[ 
          $(document).ready(function(){

            var bOddRow = false;
            $('table.rule-results tr').not('.diagnostics').each(function(){
              var jTr = $(this);
              if (jTr.hasClass('group')) {
                bOddRow = false;
                return;
              } else if (bOddRow) {
                jTr.addClass('altBg');
              }
              bOddRow = !bOddRow;
            });

            // When rule title clicked, create diagnostics View and/or toggle it's visiblity
            $('a.toggleDiagnostics').on('click', function(){

              var jA = $(this);
              var jDiagnosticsTr = jA.parent().parent().next();
              var jRuleDiagnostics = jDiagnosticsTr.find('div.xmlRuleDiagnostics');
              var jDiagnosticsView = jDiagnosticsTr.find('div.divDiagnosticsView');

              if (jDiagnosticsView.length == 0){
                jRuleDiagnostics.after('<div class="divDiagnosticsView" result="'+ jRuleDiagnostics.attr('result') +'"></div>');
                var jDiagnosticsView = jRuleDiagnostics.siblings('div.divDiagnosticsView');
                generateDiagnosticsView(jRuleDiagnostics,jDiagnosticsView);
                jDiagnosticsView.find('pre code').each(function(i, e) { hljs.highlightBlock(e)} );
              }

              if (jA.hasClass('open')) {
                jDiagnosticsTr.find('div.diagnostics-content').slideUp(function(){
                  jDiagnosticsTr.hide();
                });
                jA.removeClass('open');
              } else {
                jDiagnosticsTr.show().find('div.diagnostics-content').slideDown();
                jA.addClass('open');
              }

              return false;
            });

            // enable scrolling from logic list item to detail section
            $('tr.diagnostics').on('click','.test-title',function(){
              var jA = $(this);
              var jTarget = jA.parentsUntil('.divDiagnosticsView').filter('ul.logic').siblings("div.detail-section[rel='"+ jA.attr('rel') +"']:first");

              if (jTarget.length) {
                var iScrollTo = jTarget.offset().top - 5;
                $('html, body').animate({ scrollTop:iScrollTo }, 1000, function(){
                  jTarget.stop(true).siblings().fadeTo(2000,1);
                });
                jTarget.stop(true).fadeIn(1).siblings('.detail-section').stop(true).fadeTo(1,'0.4');
              }
              return false;
            });

            // enable view of reference attributes in detail tables
            $('tr.diagnostics').on('click','tr.reference-toggle a',function(){
              jTr = $(this).parent().parent();
              jTr.siblings('.reference').show();
              jTr.remove();
              return false;
            });
            

            // Generate markup for diagnostics view
            var arDetailSections = [];
            function generateDiagnosticsView(jData, jView) {
              var arHTML = [];
              var sLogicHTML = '';
              arDetailSections = [];

              // Create logic list overview
              var jCheck = jData.find('complex-check');
              if (jCheck.length == 0) jCheck = jData.find('check_diagnostics > check:first');

              sLogicHTML = generateLogicListItems(jCheck, jData);

              if (sLogicHTML.indexOf('<li') != sLogicHTML.lastIndexOf('<li')) {
                // result is made up of multiple results, combined

                arHTML.push('<h4 class="logic">Result Component Logic</h4>');
                arHTML.push('<ul class="logic result-'+ jView.attr('result')  +'">');
                  arHTML.push(sLogicHTML);
                arHTML.push('</ul>');

                arHTML.push('<h4>Result Component Details</h4>');
              } else {
                // result is made up of one result, no need to show combination logic
                arHTML.push('<h4>Result Details</h4>');
              }

              // Create details for each test/sce/ocil questionnaire
              var jSection;
              for (var i=0; i < arDetailSections.length; i++) {
                jSection = arDetailSections[i].check;
                switch (jSection[0].tagName.toString().toLowerCase()) {
                  case 'sce_results': arHTML.push(generateSCEDetail(jSection, jData, arDetailSections[i].negate)); break;
                  case 'questionnaire': arHTML.push(generateOCILDetail(jSection, jData, arDetailSections[i].negate)); break;
                  default: arHTML.push(generateTestDetail(jSection, jData, arDetailSections[i].negate)); break;
                }
              }

              jView.html(arHTML.join(''));
            }

            // Generate logical list (recursively)
            function generateLogicListItems(jItem, jData, oOptions){
              if (!oOptions)  oOptions = {};
              var arHTML = [];

              if (jItem.length == 0) return '<li>No item found.</li>';

              // handle negation: get current item's value, adjust based on parent, create options to pass to child
              var bNegate = getBooleanFromString(jItem.attr('negate'));
              if (oOptions.negate) bNegate = !bNegate;
              var oChildOptions = { negate:bNegate };

              var sElement = jItem[0].tagName.toLowerCase();
              var bCheck = (sElement == 'check');
              var sCheckSystem = (bCheck) ? jItem.attr('system') : '';
              var bOVALCheck = (bCheck && sCheckSystem == 'http://oval.mitre.org/XMLSchema/oval-definitions-5');
              var bOCILCheck = (bCheck && sCheckSystem == 'http://scap.nist.gov/schema/ocil/2');
              var bSCECheck = (bCheck && sCheckSystem == 'http://open-scap.org/page/SCE');

              // TODO: check@multi-check=true, ignore for now b/c only impacts scoring

              // handle checks with no supplied check-content-ref@name OR where check-content-ref'd does not exist
              if (bOVALCheck || bSCECheck || bOCILCheck) {
                var jCheckContentRefs = jItem.find('check-content-ref');
                if (jCheckContentRefs.length == 0) return '<li>No check-content-ref found.</li>';

                // remove content-refs that don't refer to a check in diagnostic info
                jCheckContentRefs.filter(function(){
                  var sCheckContentRefName = (bSCECheck) ? $(this).attr('href') : $(this).attr('name');
                  if (!sCheckContentRefName) return false;
                  // check-content-ref@name provided, but does the content exists?
                  if (bOVALCheck) var jCheckContent = jData.find("definition_results definition[definition_id='"+ sCheckContentRefName +"']");
                  if (bOCILCheck) var jCheckContent = jData.find("ocil_result_diagnostics questionnaire[id='"+ sCheckContentRefName +"']");
                  if (bSCECheck) var jCheckContent = jData.find("sce_results[script-path='"+ sCheckContentRefName +"']");
                  return (jCheckContent.length > 0);
                });

                if (jCheckContentRefs.length > 0) {
                  // we have check-content-refs that explicitly reference checks!
                  var jCheckContentRef = jCheckContentRefs.eq(0);
                  var sCheckContentRefName = (bSCECheck) ? jCheckContentRef.attr('href') : jCheckContentRef.attr('name');
                
                  if (bOVALCheck) var jCheckContent = jData.find("definition_results definition[definition_id='"+ sCheckContentRefName +"']");
                  if (bOCILCheck) var jCheckContent = jData.find("ocil_result_diagnostics questionnaire[id='"+ sCheckContentRefName +"']");
                  if (bSCECheck) var jCheckContent = jData.find("sce_results[script-path='"+ sCheckContentRefName +"']");

                  // if multiple check_diagnostics elements, set the context to appropriate check_diagnostics element
                  jData = jCheckContent.parentsUntil('xmlRuleDiagnostics').filter('check_diagnostics');
                } else {
                  // no content-refs explicitly ref a check, so look for one with no href
                  jCheckContentRefs = jItem.find('check-content-ref').not("[name]");

                  // if still no content refs, return error
                  if (jCheckContentRefs.length == 0) return '<li>No check-content-refs found that refer to a check.</li>';

                  // take 1st
                  var jCheckContentRef = jCheckContentRefs.eq(0);

                  // if multple check_diagnostics, set context by finding matching check-content-ref under a check_diagnostics
                  if (jData.find('check_diagnostics').length > 0) {
                    var sCheckContentRefHref = jCheckContentRef.attr('href');
                    var jMatchingCheckContentRef = jData.find("check-content-ref[href='"+ sCheckContentRefHref +"']").not('[name]');
                    if (jMatchingCheckContentRef.length == 0) return '<li>No check-content found.</li>';
                    jData = jMatchingCheckContentRef.eq(0).parentsUntil('xmlRuleDiagnostics').filter('check_diagnostics');
                  }

                  if (bOVALCheck) var jPossibleCheckContent = jData.find("definition_results definition");
                  if (bOCILCheck) var jPossibleCheckContent = jData.find("ocil_result_diagnostics questionnaire");
                  if (bSCECheck) var jPossibleCheckContent = jData.find("sce_results");

                  if (jPossibleCheckContent.length == 0) {
                    return '<li>No check-content found.</li>';

                  } else if (jPossibleCheckContent.length == 1) {
                    // add check-content-ref and continue
                    jCheckContent = jPossibleCheckContent;

                  } else if (jPossibleCheckContent.length > 1) {
                    // create a complex check using all defs
                    var arComplexCheck = [];
                    var sRuleResult = jItem.parentsUntil('.xmlRuleDiagnostics').parent().attr('rule-result');
                    arComplexCheck.push('<complex-check operator="AND" negate="'+ bNegate.toString() +'" result="'+ sRuleResult +'">');
                      jPossibleCheckContent.each(function(){
                        if (bOVALCheck) var sCheckContentRefName = $(this).attr('definition_id');
                        if (bOCILCheck) var sCheckContentRefName = $(this).attr('id');
                        if (bSCECheck) var sCheckContentRefName = $(this).attr('script-path');                  
                        arComplexCheck.push('<check system="'+ sCheckSystem +'"><check-content-ref name="'+ sCheckContentRefName  +'" /></check>');
                      });
                    arComplexCheck.push('</complex-check>');

                    // append our new complex-check under xmlRuleDiagnostics
                    var jComplexCheck = $(arComplexCheck.join());
                    jItem.parentsUntil('.xmlRuleDiagnostics').parent().append(jComplexCheck);

                    // process complex-check and abort
                    arHTML.push(generateLogicListItems(jComplexCheck, jData, oChildOptions));
                    return arHTML.join('');
                  }
                }
              }

              // <check system="http://oval.mitre.org/XMLSchema/oval-definitions-5">
              if (bOVALCheck) {
                // Inverse meaning of definition result for patch and vulnerability (e.g. false means PASS)
                var sClass = jCheckContent.attr('class');
                if (sClass == 'patch' || sClass == 'vulnerability') oChildOptions.negate = !oChildOptions.negate;

                // Get ref to criteria and pass to generateLogicListItems
                var jCriteria = jCheckContent.children('criteria');
                if (jCriteria.length == 0) return '<li>No criteria found in defition element.</li>';
                arHTML.push(generateLogicListItems(jCriteria, jData, oChildOptions));
              }

              // <check system="http://scap.nist.gov/schema/ocil/2">
              if (bOCILCheck) {
                var sResult = getIdString(jCheckContent.attr('result'));
                var sQuestionnaireTitle = jCheckContent.children('title').text();
                var sContentId = getIdString(jCheckContent.attr('id'));

                arHTML.push('<li class="ocil result-'+sResult+'">');
                  arHTML.push('<span class="ocil-title" rel="ocil-'+ sContentId +'">'+ sQuestionnaireTitle +'<small>OCIL Questionnaire</small></span>');
                arHTML.push('</li>');

                arDetailSections.push({check:jCheckContent, negate:bNegate});
              }

              // <ns2:check system="http://open-scap.org/page/SCE" negate="false" selector="" multi-check="false">
              if (bSCECheck) {
                var sResult = getIdString(jCheckContent.children("result").text());
                var sContentId = getIdString(jCheckContent.attr('script-path'));

                arHTML.push('<li class="sce result-'+sResult+'">');
                  arHTML.push('<span class="sce-title" rel="sce-'+ sContentId +'">'+ sContentId +'<small>SCE script</small></span>');
                arHTML.push('</li>');

                arDetailSections.push({check:jCheckContent, negate:bNegate});
              }

              // <complex-check operator="AND" negate="false" result="true">
              // <criteria operator="AND" negate="false" result="true">
              if (sElement == 'criteria' || sElement == 'complex-check'){
                var sResult = getIdString(jItem.attr('result'));
                var sGroupLabel = getGroupLabel(jItem, bNegate);
                var jChildren = jItem.children();

                arHTML.push('<li class="group result-'+sResult+'">');
                  arHTML.push('<span class="operator">' +sGroupLabel+ '</span>');
                  if (oOptions.extend_definition) arHTML.push('<small>Extend Definition: '+ oOptions.extend_definition +'</small>');
                  arHTML.push('<ul>')
                    for (var i=0; i < jChildren.length; i++) arHTML.push(generateLogicListItems($(jChildren[i]), jData, oChildOptions));
                  arHTML.push('</ul>');
                arHTML.push('</li>');
              }

              // <extend_definition definition_ref="oval:gov.nist.cpe.oval:def:1" comment="Windows 7 is installed"></extend_definition>
              if (sElement == 'extend_definition'){
                var sDefinitionId = jItem.attr('definition_ref');
                var sComment = jData.find("definitions extend_definition[definition_ref='"+ sDefinitionId +"']:first").attr('comment');
                if (sComment == '') sComment = jData.find("definitions definition[id='"+ sDefinitionId +"'] title").text();
                if (sComment == '') sComment = sDefinitionId;
                oChildOptions.extend_definition = sComment;

                var jDefinition = jData.find("definition_results definition[definition_id='"+ sDefinitionId +"']");
                if (jDefinition.length == 0) return '<li>Unable to find definition for id: '+sDefinitionId+ '</li>';;

                var sResult = getIdString(jDefinition.attr('result'));

                var jCriteria = jDefinition.children("criteria");
                if (jCriteria.length == 0) return '<li>Unable to find criteria for id: '+sDefinitionId+ '</li>';;

                arHTML.push(generateLogicListItems(jCriteria, jData, oChildOptions));
                
                //arHTML.push('<li class="extend-definition group result-'+sResult+'">' +sComment+ '<ul>');
                  //arHTML.push(generateLogicListItems(jCriteria, jData));
                //arHTML.push('</ul></li>'); 
              }

              // <criterion test_ref="oval:org.mitre.oval:tst:999" negate="false" result="true"></criterion>
              if (sElement == 'criterion'){
                var sResult = getIdString(jItem.attr('result'));
                var sTestId = jItem.attr('test_ref');
                var jTest = jData.find("tests [id='"+ sTestId +"']");
                if (jTest.length == 0) return '<li>Unable to find test for id: '+sTestId+ '</li>';;
                var sComment = jTest.attr('comment');
                if (sComment == '') sComment = sTestId;
                if (bNegate) sComment = 'Not: ' + sComment;

                arHTML.push('<li class="test result-'+sResult+'">');
                  arHTML.push('<span class="test-title" rel="test-'+ sTestId +'">'+ getUCFirst(sComment) +'<small>OVAL Test</small></span>');
                arHTML.push('</li>');

                arDetailSections.push({check:jTest, negate:bNegate});
              }

              return arHTML.join('');
            }

            function generateSCEDetail(jResults, jData, bNegate) {
              var arHTML = [];

              var sTitle = jResults.attr('script-path');
              var sScriptId = getIdString(sTitle);
              var sResult = getIdString(jResults.children('result').text());    

              // Parse environment Variables
              var arEnvVars = [];
              jResults.find('environment > entry').each(function(){
                var sToken = $.trim($(this).text());
                var i = sToken.indexOf('=');
                if (i == -1) return false;
                arEnvVars.push({
                  name : sToken.substring(0,i),
                  value : sToken.substring(i+1)
                });
              });

              // Markup
              arHTML.push('<div class="detail-section sce result-'+sResult+'" rel="sce-'+ sScriptId +'">');
                arHTML.push('<h5 class="result-'+sResult+'">'+ getUCFirst(sTitle) +': '+ getUnIdString(sResult).toUpperCase() +'</h5>');

                // Output environment Variables
                if (arEnvVars.length > 0){
                  arHTML.push('<h6>SCE Environment Variables</h6>');

                  arHTML.push('<table class="value-detail">');
                    arHTML.push('<tr class="header"><td>Name</td><td>Value</td></tr>');
                    for (var iEVar=0; iEVar < arEnvVars.length; iEVar++) {
                      arHTML.push('<tr><td>'+ arEnvVars[iEVar].name +'</td><td>'+ arEnvVars[iEVar].value +'</td></tr>');
                    }
                    arHTML.push('</table>');
                }

                // Output exit code
                arHTML.push('<h6>SCE Exit Code</h6><pre><code class="no-highlight">'+ escapeHtml(jResults.children('exit_code').html()) +'</code></pre>');

                // Output standard out
                arHTML.push('<h6>SCE Standard Out</h6><pre><code class="dos">'+ formatStdOutString(jResults.children('stdout').text()) +'</code></pre>');
              arHTML.push('</div>');
               
              return arHTML.join('');
            }

            function generateOCILDetail(jQuestionnaire, jData, bNegate) {
              var arHTML = [];

              var sQuestionnaireId = jQuestionnaire.attr('id');
              var sTitle = jQuestionnaire.children('title').text();
              var sResult = getIdString(jQuestionnaire.attr('result'));

              arHTML.push('<div class="detail-section oval result-'+sResult+'" rel="ocil-'+ sQuestionnaireId +'">');
                arHTML.push('<h5 class="result-'+sResult+'">'+ getUCFirst(sTitle) +': '+ getUnIdString(sResult).toUpperCase() +' <small>'+ sQuestionnaireId +'</small></h5>');
                // TODO: recursively display OCIL Diagnostics nicely
                arHTML.push('<pre><code>&lt;!-- OCIL Diagenostic XML --&gt\n'+ escapeHtml(jQuestionnaire.parent().html()) +'</code></pre>');
              arHTML.push('</div>');
               
              return arHTML.join('');
            }

            function generateTestDetail(jTest, jData, bNegate) {
              var arHTML = [];

              // get test id, title, result
              var sTestId = jTest.attr('id');
              var sComment = jTest.attr('comment');
              if (sComment == '') sComment = sTestId;
              var jTestResult = jData.find("test_results test[test_id='"+ sTestId +"']");
              if (jTestResult.length == 0) return '<div class="processing-error">Unable to find test results for: '+ sTestId +'</div>';

              var sResult = getIdString(jTestResult.attr('result'));
              var sTestMessage = getMessageText(jTestResult);

              // convert @check_existence to friendly description
              var sCheckExistense = getIdString(jTest.attr('check_existence'));
              if (sCheckExistense == '') sCheckExistense = 'at_least_one_exists';
              var sCheckExistenceDescription = {
                all_exist: "Every item must exist.",
                any_exist: "",
                at_least_one_exists: "At least one item must exist.",
                none_exist: "The item must NOT exist.",
                only_one_exists: "Exactly one item must exist."
              }[sCheckExistense];

              // convert @check to friendly description
              var sCheck = getIdString(jTest.attr('check'));
              var bHasStates = (jTest.find('state').length > 0);
              var sCheckDescription = {
                all: "All items found must satisfy the expected state.",
                at_least_one: "At least one item found must satisfy the expected state.",
                none_exist: "No item found may satify the expected state.", 
                none_satisfy: "No item found may satify the expected state.", 
                only_one: "Exactly one item must satify the expected state."
              }[sCheck];

              // convert @state_operator att to friendly description        
              var sStateOperator = getIdString(jTest.attr('state_operator'));
              if (sStateOperator == '') sStateOperator = 'and';
              var sStateOperatorDescription = {
                and: "Conditions must match all of the states.",
                one: "Conditions must match one of the states.",
                or: "Conditions must match at least one of the states.",
                xor: "Conditions must match an odd number of the states."
              }[sStateOperator.toLowerCase()];

              // get collected object metadata
              var jObject = jTest.find('object');
              if (jObject.length == 0) return '<div class="processing-error">Unable to find object for test: '+ sTestId +'</div>';
              var sObjectId = jObject.attr('object_ref');
              var jCollectedObject = jData.find("collected_objects object[id='"+ sObjectId +"']");
              if (jCollectedObject.length == 0) return '<div class="processing-error">Unable to find collected object for: '+ sObjectId +'</div>';

              // convert object@flag to friendly description
              var sObjectFlag = getIdString(jCollectedObject.attr('flag'));
              if (sObjectFlag == '') sObjectFlag = 'complete';
              var sObjectCollectionIssue = {
                error: "An error occurred during object collection.",
                complete: false,
                incomplete: "Additional matching objects may exist.",
                does_not_exist: "No matching objects were found.",
                not_collected: "No attempt was made to collect matching objects.",
                not_applicable: "The object is not applicable to the system under test."
              }[sObjectFlag];

              var sObjectMessage = getMessageText(jCollectedObject);

              // get collected items array [{element_name:collected_value, element_name:collected_value},{etc.}]
              var arItems = [];
              jTestResult.children('tested_item').each(function(){
                var jTestedItem = $(this);
                var sTestedItemResult = getIdString(jTestedItem.attr('result'));
                var jCollectedItem = jData.find("items > [id='"+ jTestedItem.attr('item_id') +"']");
                if (jCollectedItem.length == 0) return '<div class="processing-error">Unable to find tested items for object: '+ sObjectId +'</div>';
                var sItemTagName = jCollectedItem[0].tagName.toString().toLowerCase();
                var oItem = {
                  _tested_item_result: sTestedItemResult,
                  _item_element_name: sItemTagName,
                  _is_complex_datatype: (sItemTagName == 'wmi57_item' || sItemTagName == 'wmi_item')
                }
                if (oItem._is_complex_datatype) {
                  oItem._xml = jCollectedItem[0].outerHTML;
                } else {
                  oItem = getChildrenAsObject(jCollectedItem, oItem);
                }
                arItems.push(oItem);
                
              });
              //console.log(arItems);    

              // get states: [{entityName:{operation:"greater than",value:5},entityName:{operation:"less than",value:2}, {2nd state...}}]
              var arStates = [];
              if (bHasStates) {
                jTest.children('state').each(function(){
                  var jState = jData.find("states > [id='"+ $(this).attr('state_ref') +"']");
                  if (jState.length == 0) return '<div class="processing-error">Unable to find state: '+ $(this).attr('state_ref') +'</div>';
                  var sStateTagName = jState[0].tagName.toString().toLowerCase();
                  var oState = {
                    _id: jState.attr('id'),
                    _state_element_name: sStateTagName,
                    _is_complex_datatype: (sStateTagName == 'wmi57_state' || sStateTagName == 'wmi_state')
                  };

                  if (oState._is_complex_datatype) {
                    oState._xml = jState[0].outerHTML;
                  } else {
                    jState.children().each(function(){
                      var jElement = $(this);
                      var sVarRef = jElement.attr('var_ref');
                      if (sVarRef) {
                        var jTestedValue = jTestResult.children("[variable_id='"+ sVarRef +"']");
                        if (jTestedValue.length == 0) return '<div class="processing-error">Unable to find tested_variable for: '+ sVarRef +'</div>';

                        if (jTestedValue.length == 1) {
                          // One variable value
                          var sValue = jTestedValue.text();
                        } else {
                          // Multiple values: concatenate in a phrase
                          var sVarCheck = jElement.attr('var_check');
                          sVarCheck = (sVarCheck) ? sVarCheck.toLowerCase() : 'all';
                        
                          var arMutliValues = [];
                          jTestedValue.each(function(){ arMutliValues.push($(this).text()); });

                          var sValue = sVarCheck + ' of: '+ arMutliValues.join(', ');
                        }
                      } else {
                        var sValue = jElement.text();
                      }

                      var sOperation = jElement.attr('operation');

                      oState[this.tagName.toString().toLowerCase()] = {
                        operation: (sOperation) ? sOperation : 'equals',
                        value: sValue
                      }
                    });
                    arStates.push(oState);
                  }

                });
              }
              //console.log(arStates);

              // TODO: handle large number of statea/items       

              // output detail section
              var sEntityName = '';

              // if test is negated, wrap it in negated container
              if (bNegate) {
                var sNegatedResult = getNegatedResult(sResult);
                arHTML.push('<div class="detail-section result-'+ sNegatedResult +'" rel="test-'+ sTestId +'">');
                arHTML.push('<h5 class="result-'+sNegatedResult+'">Negated to: '+ getUnIdString(sNegatedResult).toUpperCase() +'</h5>');
              }

              arHTML.push('<div class="detail-section result-'+sResult+'" rel="test-'+ sTestId +'">');

                arHTML.push('<h5 class="result-'+sResult+'">'+ getUCFirst(sComment) +': '+ getUnIdString(sResult).toUpperCase() +' <small>'+ sTestId +'</small></h5>');
                arHTML.push('<div class="existence-stateop">');
                  arHTML.push(sCheckExistenceDescription);
                  if (bHasStates) arHTML.push( (sCheckExistenceDescription != '') ? (' And, '+ sCheckDescription.toLowerCase()) : sCheckDescription);
                arHTML.push('</div>');

                if (sTestMessage) arHTML.push('<h6>Test Execution Message</h6><pre><code class="dos">'+ sTestMessage +'</code></pre>');
                if (sObjectCollectionIssue) arHTML.push('<h6>Object Collection Message</h6><pre><code class="dos">'+ sObjectCollectionIssue +'</code></pre>');
                if (sObjectMessage) arHTML.push('<h6>Object Message</h6><pre><code class="dos">'+ sObjectMessage +'</code></pre>');

                // multiple states?
                var iTotalStates = arStates.length;
                var iTotalItems = arItems.length;
                if (iTotalStates == 0 && iTotalItems == 0){
                  arHTML.push('<div class="items-state-message">This test did not specify any expected values and did not find any items.</div>');
                } else if (iTotalStates > 0 && iTotalItems == 0) {
                  arHTML.push('<div class="items-state-message">This test did not find any items.</div>');
                } else if (iTotalStates == 0 && iTotalItems > 0) {
                  // items, but no states so just output items
                  arHTML.push('<div class="items-state-message">This test did not specify any expected values, but did find '+iTotalItems+' item(s).</div>');

                  for (var iItem=0; iItem < arItems.length; iItem++){
                    arHTML.push('<table class="value-detail">');

                    oItem = arItems[iItem];
                    arHTML.push('<tr class="header">');
                      arHTML.push('<td>'+ getUCWords(oItem._item_element_name.replace(/_/g,' ')) +'</td><td>Actual Value</td>');
                    arHTML.push('</tr>');
                    if (oItem._is_complex_datatype) {
                      // for complex types, just dump XML
                      arHTML.push('<tr><td colspan="2"><pre><code>&lt;!-- ComplexDataType: Item XML --&gt\n'+ escapeHtml(oItem._xml) +'</code></pre></td>');
                    } else {
                      // output entities
                      for (sEntityName in oItem){
                        if (sEntityName.charAt(0) == '_') continue;
                        arHTML.push('<tr class="reference"><td>'+sEntityName+'</td><td>'+oItem[sEntityName]+'</td></tr>');
                      }
                    }
                    
                    arHTML.push('</table>');
                  }
                  

                } else {
                  // items and 1+ states, so output all states/items

                  if (iTotalStates > 1) arHTML.push('<div class="items-state-message">This test has '+ iTotalStates +' expected states and found '+iTotalItems+' item(s). '+sStateOperatorDescription +'</div>');
                  // output details for eachitem
                  for (var iState=0; iState < arStates.length; iState++){
                    oState = arStates[iState];
                    arHTML.push('<h6>'+ getUCWords(oState._state_element_name.replace(/_/g,' ')) +' <small>'+ oState._id +'</small></h6>');
                    
                    for (var iItem=0; iItem < arItems.length; iItem++){
                      arHTML.push('<table class="value-detail">');

                      oItem = arItems[iItem];
                      // console.log('----------------');console.log(oItem);console.log(oState);
                      
                      if (oState._is_complex_datatype) {
                        // for complex types, just dump XML
                        arHTML.push('<tr><td colspan="4"><pre><code class="xml">&lt;!-- ComplexDataType: State XML --&gt\n'+ escapeHtml(oState._xml) +'</code></pre></td>');
                        arHTML.push('<tr><td colspan="4"><pre><code class="xml">&lt;!-- ComplexDataType: Item XML --&gt\n'+ escapeHtml(oItem._xml) +'</code></pre></td>');
                      } else {
                        arHTML.push('<tr class="header">');
                          arHTML.push('<td>'+ getUCWords(oItem._item_element_name.replace(/_/g,' ')) +'</td>');
                          arHTML.push('<td>Actual Value</td><td>Operation</td><td>Expected Value</td>');
                        arHTML.push('</tr>');
                        
                        // output entities tested in state
                        for (sEntityName in oState){
                          if (sEntityName.charAt(0) == '_') continue;
                          arHTML.push('<tr><td>'+sEntityName+'</td><td>'+oItem[sEntityName]+'</td>');
                          arHTML.push('<td>'+oState[sEntityName].operation+'</td><td>'+oState[sEntityName].value+'</td></tr>');
                        }
                        // output entities not tested in state
                        var bReferenceAttHeader = false;
                        for (sEntityName in oItem){
                          if (sEntityName in oState) continue;
                          if (sEntityName.charAt(0) == '_') continue;

                          if (!bReferenceAttHeader) {
                            bReferenceAttHeader = true;
                            arHTML.push('<tr class="reference-toggle"><td colspan="4"><a href="#">show untested values</a></td></tr>');
                          }

                          arHTML.push('<tr class="reference hidden"><td>'+sEntityName+'</td>');
                          arHTML.push('<td colspan="3">'+oItem[sEntityName]+'</td></tr>');
                        }
                      }

                      arHTML.push('</table>');
                    }
                    

                  }
                
                }

              arHTML.push('</div>');
              if (bNegate) arHTML.push('</div>');

              return arHTML.join('');
            }

            function getNegatedResult(sResult) {
              switch (sResult) {
                case 'true': return 'false';
                case 'false': return 'true';
              }
              return sResult;
            }

            function getMessageText(jParent) {
              var jMessage = jParent.children('message');
              if (jMessage.length == 0) return false;
              var arHTML =[];
              jMessage.each(function(){
                var jMessage = $(this);
                var sLevel = jMessage.attr('level');
                if (!sLevel) sLevel = 'info';
                arHTML.push(sLevel.toUpperCase() +': '+ jMessage.text());
              });
              return arHTML.join('; ') + '.';
            }

            function getGroupLabel(jGroup, bNegateNegate){
              var sOperator = jGroup.attr('operator').toLowerCase();
              if (sOperator == '') sOperator = 'and';
              var bNegate = getBooleanFromString(jGroup.attr('negate'));
              if (bNegateNegate) bNegate = !bNegate;

              switch (sOperator) {
                case 'and': return (bNegate) ? 'Not All Of' : 'All of';
                case 'one': return (bNegate) ? 'Not One Of' : 'One of';
                case 'or': return (bNegate) ? 'None Of' : 'At Least One Of';
                case 'xor': 
                  switch (jGroup.children().length) {
                    case 1: return (bNegate) ? 'None of' : 'All of';
                    case 2: return (bNegate) ? 'Both or Neither of' : 'One of';
                    case 3: return (bNegate) ? 'None or Two of' : 'One or All of';
                    default: return (bNegate) ? 'An Even Number of' : 'An Odd Number of';
                  }
              } 

              return 'Unknown Operator';
            }

            function getChildrenAsObject(jParent, oExtend) {
              var oReturn = (oExtend) ? oExtend : {};
              jParent.children().each(function(){
                oReturn[this.tagName.toString().toLowerCase()] = $(this).text();
              });
              return oReturn;
            }

            function getBooleanFromString(s, bDefaultTrue) {
              if (typeof s == 'undefined') return (typeof bDefaultTrue == 'boolean' && bDefaultTrue);
              s = s.toLowerCase();
              if (bDefaultTrue) return (s !== 'false' && s !== '0');
              return (s === 'true' || s === '1');
            }

            function getIdString(s){
              if (typeof s == 'undefined') s = '';
              return s.replace(/\W/g,'_').toLowerCase();
            }

            function getUnIdString(s){
              return s.replace(/_/g,' ');
            }

            function getUCFirst(s) {
              return s.charAt(0).toUpperCase() + s.slice(1);
            }

            function getUCWords(s) {
              var arWords = s.split(' ');
              var arReturn = [];
              for (var i=0; i<arWords.length; i++){
                arReturn.push(getUCFirst(arWords[i]));
              }
              return arReturn.join(' ');
            }

            function escapeHtml(s) {
              return resetIndent(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
           }

           function resetIndent(s) {
              s = s.replace(/^\s*$[\n\r]{1,}/gm, '');
              s = s.replace(/\s*$/, '');
              var arBaseIndent = s.match(/^\s+/);
              if (arBaseIndent) {
                var rxBaseIndent = new RegExp('^'+arBaseIndent[0],'gm');
                s = s.replace(rxBaseIndent,'');
              }
              return s;
           }

           function removeIndent(s) {
              s = s.replace(/^\s*$[\n\r]{1,}/gm, '');
              s = s.replace(/\s*$/, '');

              return s.replace(/^\s+/gm,'')
           }

            function formatStdOutString(s){
              s = s.replace(/^\s*/, '');
              s = s.replace(/\s*$/, '');

              return s.replace(/\s*\r+\s*/g,'<br/>').replace(/\s+/g,' ');
            }



          });
        ]]>
    </script>
  </xsl:template>

  <!-- Utility Formatting Templates -->

  <xsl:template name="RemoveNS">
    <xsl:param name="sourceElt"/>
    <xsl:for-each select="$sourceElt">
      <!-- remove element's ns prefix -->
      <xsl:element name="{local-name()}">
        <!-- remove attributes' ns prefix -->
        <xsl:for-each select="@*">
          <xsl:attribute name="{local-name()}">
            <xsl:value-of select="."/>
          </xsl:attribute>
        </xsl:for-each>
        
        <!-- add elt value, trimmed unless stdout: only if no children? -->
        <xsl:choose>
          <xsl:when test="local-name() = 'stdout' or local-name() = 'comment'">
            <xsl:value-of select="./text()"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="normalize-space(./text())"/>
          </xsl:otherwise>
        </xsl:choose>

        <!-- apply to children -->
        <xsl:for-each select="./*">
          <xsl:call-template name="RemoveNS">
            <xsl:with-param name="sourceElt" select="."/>
          </xsl:call-template>
        </xsl:for-each>
      </xsl:element>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printDateTime">
    <xsl:param name="date"/>
    <xsl:call-template name="printDate">
      <xsl:with-param name="date" select="$date"/>
    </xsl:call-template>
    at
    <xsl:call-template name="printTime">
      <xsl:with-param name="date" select="$date"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="printDate">
    <xsl:param name="date"/>
    <xsl:variable name="year">
      <xsl:value-of select="substring-before($date, '-')"/>
    </xsl:variable>
    <xsl:variable name="mon">
      <xsl:value-of select="substring-before(substring-after($date, '-'), '-') - 1"/>
    </xsl:variable>
    <xsl:variable name="months">
      <xsl:value-of select="'Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec'"/>
    </xsl:variable>
    <xsl:variable name="month">
      <xsl:value-of select="substring($months, $mon * 4, 4)"/>
    </xsl:variable>
    <xsl:variable name="day">
      <xsl:value-of select="substring-before(substring-after(substring-after($date, '-'), '-'), 'T')"/>
    </xsl:variable>
    <xsl:value-of select="concat($day, ' ', $month, ' ', $year)"/>
  </xsl:template>

  <xsl:template name="printTime">
    <xsl:param name="date"/>
    <xsl:variable name="hh">
      <xsl:value-of select="format-number(substring-before(substring-after($date, 'T'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="mm">
      <xsl:value-of select="format-number(substring-before(substring-after($date, ':'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="ss">
      <xsl:value-of select="format-number(substring-before(substring-after(substring-after($date, ':'), ':'), '.'), '00')"/>
    </xsl:variable>
    <xsl:value-of select="concat($hh, ':', $mm, ':', $ss)"/>
  </xsl:template>
</xsl:stylesheet>
