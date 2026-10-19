package com.maptec.applied.demo

import androidx.test.platform.app.InstrumentationRegistry
import com.maptec.applied.search.model.common.EvaluationMode
import com.maptec.applied.search.model.request.AlongRouteSearchRequest
import com.maptec.applied.search.model.request.RouteGeometry
import com.maptec.applied.search.model.response.ResponseStatus
import com.maptec.applied.search.service.SearchService
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue

/**
 * 沿途搜索联调前置：真实探测 `/search/v1/alongRoute` 是否可用。
 *
 * 服务端 4xx（网络错误）会走 [Result.failure]；业务错误码（如 `10005 Resource not found`）
 * 是 HTTP 200 + `status=ERROR`，需要按 [ResponseStatus] 判断。
 *
 * 当前状态：测试节点已发布 alongRoute，正式节点未发布。
 * 测试环境跑真实用例；正式环境探测失败自动 skip，避免假绿。
 */
internal object AlongRouteSearchAuth {

    private const val ALONG_ROUTE_RESOURCE = "/search/v1/alongRoute"

    private var probeResult: Boolean? = null
    private var standardProbeResult: Boolean? = null

    fun assumeAlongRouteAuthorized() {
        val authorized = probeResult ?: probe(evaluationMode = null).also { probeResult = it }
        assumeTrue(
            "Demo API Key 未授权 $ALONG_ROUTE_RESOURCE（当前节点未发布该接口）。" +
                "发布后本用例会自动启用。",
            authorized,
        )
    }

    fun assumeStandardEvaluationAvailable() {
        assumeAlongRouteAuthorized()
        val available = standardProbeResult
            ?: probe(evaluationMode = EvaluationMode.STANDARD).also { standardProbeResult = it }
        assumeTrue(
            "当前节点未开通 alongRoute STANDARD 评估（10350 Standard route evaluation unavailable）。" +
                "开通后本用例会自动启用。",
            available,
        )
    }

    private fun probe(evaluationMode: EvaluationMode?): Boolean = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val searchService = SearchService.getInstance(context)
        val request = AlongRouteSearchRequest(
            query = "hotel",
            route = RouteGeometry(encodedPolyline = SAMPLE_ENCODED_POLYLINE),
            routeBufferMeters = 1000,
            evaluationMode = evaluationMode,
            resultLimit = 1,
        )
        val response = searchService.alongRouteSearch(request).getOrNull() ?: return@runBlocking false
        response.status != ResponseStatus.ERROR
    }

    private const val SAMPLE_ENCODED_POLYLINE =
        "qoyF_k|xRRFbCr@XT??HFBDDDBPUb@Ud@OKBER]?OAGY]YKc@Mw@Uk@OoCe@_CY_CSo@Eo@CiC@cLK{BAgLGu@AiAG_AM}@Qs@Uw@]cAu@{@{@o@_AUe@[{@SaAQaAOiAo@cGYyBqDcYSeAM_AWoEMqF_@sJMs@Qe@W_@WUSMQM[Ke@OcBg@aBi@cBi@eA]_AYsAa@a@Me@Oa@Ua@Ki@OcAWq@Qm@Kg@Ic@EOAQAYC[Aa@A[?Y@]?Q@U@Q@YBYDYDYDYDYFi@Ny@TkAZ}Ab@{Ab@uBl@cAXsAZq@TkBj@??gAXe@Nk@N??UF??MDe@La@J??WF]H]H]Ha@He@JSDc@HG@i@Jg@Hi@Jm@Hg@Hm@Hi@Hs@Hi@Fg@FeAJc@Bg@Dm@Fg@BWBa@Be@Be@Bg@BWBk@BeAHk@BQ@a@Bg@Bc@Bc@@a@@a@?[?i@Ag@Ag@Ca@Cc@Cc@Ee@Gc@Gc@Ia@Ia@Ma@Ma@MSIMEUMIE??GCUM_@USOc@]WWSSWYOWQUU][i@MYKUISO]ISQa@IQMYGMQc@Sc@Ui@Qa@KSGOGQMYO[MYKWIQGMKWQe@IQISUo@Ma@Qm@Qk@GSI]IUIWI[Ma@Ok@K[I[GSSo@EMK[Qc@KWISGKKSIMIKIMGKQQIMMKIKKKKIOMYSMIKGKGOIMGSKMEOGSIMESEMEOEKCOCOCQCSCOAMCI?IAIAQASCQASAQAYAQAOCK?SAQAMAMAG?GAI?OASAG?IAI?MAIAE?O?EAK?MAK?OAM?E?GAI?OAQ?QAS?QAG?O?OAS?Q?WAO?YAQ?MASAIAOAKAMAOCGAMCMCICICMEMEOGWMMG[QSOUSOOKKKMOQKQQYIQIOKWSe@O[M[EKQ_@ISUe@_@w@Yi@Wg@[m@Ye@O[QYKSIOQYQ[ISUa@IQKUO[IQIOEMKUGOKWSg@ACMg@MWEMGQGWGOIWEOOg@K[MYK]MYQe@a@}@Ug@Ue@Q[OUO_@KOGMCEOSQOKSYc@]g@a@k@Ya@W]KQs@y@o@u@Y[QSUUKIKKAC]YSSOMYWOKWS_@[c@[MIWQYS]SQKYQg@YUMc@U[QUKUK[MQIGCEC]MYMe@Oi@O_@Kw@Q??m@QmAY_@ISEQEQCSEQCOCMCOASCUCKAOAMAWCOASAa@AUAU?U?S?_@?Y@Q?YB[@a@Da@DK@M@MBM@OBMBWDQDyAZSBODUDOBMBQ@MBM@QBM@C?UBI@G?O@I?M@K?M@U@_@?_@?m@AS?MAWAUCYCa@EUCSC_@G[GYGMCUGICKEOEmBk@oFcBw@Uy@W{@WaJkC_@MUIe@O]M_@OUI_@Os@[mGwCgMoF_Ac@uBeA{BcAsFeCsFgCuF{By@YcA]eA[gCm@eGoA]Mc@KiAYmCm@yGyAeMuC}A_@_GmAiA[qAc@mAg@{@a@KGOIoB_BqAoAuAkBqA}B}C{Ee@u@m@s@y@o@}@a@a@Og@Ku@K[C_@Ay@Aa@E_BKO@}@I_AMqAYuAUSCcAIs@@o@Bs@Fu@JoA\\k@RULkAl@m@\\o@b@s@f@WNKFOBk@d@yIdHqB`BwAhAwOjMMLaBxAgCzBqCvC{@hAk@n@eAlAaAjAw@dA_D`FuCnFGb@Sn@e@tA_@`ASf@GR@V{@fCqBlHAf@m@lEEb@EZMdAA@UrB?h@B\\P^XXVVrElAD?nBh@bAVdBb@^J^HVHTFVFzBn@tAZb@JfDv@\\H\\HVHIVI^I^_A|DCLA@W`ACJQ|@Gb@Q`BVj@RXjAHfCVFYj@eCLk@b@kBFIJAf@J"
}
