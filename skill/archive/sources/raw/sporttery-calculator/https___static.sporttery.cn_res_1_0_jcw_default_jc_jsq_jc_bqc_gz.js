
function init() {
    oddsName = { "胜胜": "0", "胜平": "1", "胜负": "2", "平胜": "3", "平平": "4", "平负": "5", "负胜": "6", "负平": "7", "负负": "8" };
    oddsIndex = ["胜胜", "胜平", "胜负", "平胜", "平平", "平负", "负胜", "负平", "负负"];
    curPool = "hafu";
};
$(document).ready(function() {
    lotFunc.ready();
    //
    $("#mainTbl").on("click","span.oddsItem", function(e) {
        if ($(this).hasClass("oddsDis")) return;
        if ($(this).text() == "" || $(this).text() == "--") return;
        if ($(this).hasClass("oddsEffect")) return;
        var index = $(this).closest("td").find("span").index($(this));
        var tmpAry = $(this).closest("tr").attr("id").split("_");
        var idStr = tmpAry[1];
        if ($(this).hasClass("oddsClk")) { //不选
            $(this).removeClass("oddsClk");
            if (selAry[idStr] == undefined) {

            } else {
                var oddsAry = selAry[idStr].odds;
                oddsAry[index] = "";
                if (Number(oddsAry.join("")) == 0) {
                    delete selAry[idStr];
                    selAryLen--;
                }
            }
        } else {
            if (selAry[idStr] == undefined) {
                var allSingle = lotFunc.isAllSingle();
                if ($(this).closest("tr").find(".vsTd").css("background-image") == "none") {
                    allSingle = false;
                }
                //超过n场
                if (selAryLen >= 4) {
                    if (!allSingle) {
                        alert("超过4场只能选择单关进行计算");
                        return;
                    }
                }
                //
                selAry[idStr] = new Object();
                selAry[idStr].odds = ["", "", "", "", "", "", "", "", ""];
                selAry[idStr].dataIndex = $(this).closest("tr").attr("dataIndex");
                selAry[idStr].pool = "hafu";
                if ($(this).closest("tr").find(".vsTd").css("background-image") != "none") {
                    selAry[idStr].single = true;
                } else {
                    selAry[idStr].single = false;
                }
                selAryLen++;
            }
            selAry[idStr].matchNumDate = $(this).closest("tr").attr("matchNumDate");
            selAry[idStr].taxDateNo = $(this).closest("tr").attr("taxDateNo");
            selAry[idStr].odds[index] = $(this).text();
            $(this).addClass("oddsClk");
            lotFunc.animate($(this).offset().top, $(this).offset().left, $(this).width(), $(this).height());
        }
        lotFunc.calculate();
        //
        //
        if ($("#optionTip").html() != "") {
            $("#optionTip").width(100);
            $("#optionTip").animate({ width: "280px" }, { easing: 'easeOutBounce', duration: 600, complete: null });
        }
    });
    //
    $("#mainTbl").on("mouseenter","span.oddsItem", function() {
        var index = $(this).closest("td").find(".oddsItem").index($(this));
        $("#headerTr span.oddsHeader:eq(" + index + ")").addClass("headerOver");
    });
    //
    $("#mainTbl").on("mouseleave", "span.oddsItem",function() {
        var index = $(this).closest("td").find(".oddsItem").index($(this));
        $("#headerTr span.oddsHeader:eq(" + index + ")").removeClass("headerOver");
    });
    //
    $(document).on("click","#mainTbl a.hafuSel", function() {
        if ($("#hfSelOption:visible").length == 0) {
            $("#hfSelOption").show();
            $(this).focus();
            $("#hfSelOption li:eq(0)").text($(this).attr("txt"));
            $("#hfSelOption").attr("target", $(this).closest("tr").attr("id") + " a.hafuSel:eq(" + $(this).index() + ")");
        } else {
            $("#hfSelOption").hide();
        }
        $("#hfSelOption").offset({ left: $(this).offset().left, top: $(this).offset().top + 18 });
    });
    $(document).on("blur","#mainTbl a.hafuSel", function() {
        setTimeout(function() { $("#hfSelOption").hide(); }, 300);
    });
    //
    $("#hfSelOption li").mouseover(function() {
        $(this).addClass("selOptionOver");
    });
    $("#hfSelOption li").mouseleave(function() {
        $(this).removeClass("selOptionOver");
    });
    //
    $("#hfSelOption li").click(function() {
        //检测是否超场数
        //超过n场
        if (selAryLen >= 4) {
            if (!lotFunc.isAllSingle()) {
                alert("超过4场只能选择单关进行计算");
                return;
            }
        }
        //
        var target = $("#" + $("#hfSelOption").attr("target"));
        target.text($(this).text());
        var trObj = target.closest("tr");
        //
        var index = 0;
        if (target.attr("txt") == "半场") {
            index = 1;
        }
        var target1 = target.closest("td").find("a:eq(" + index + ")");

        var str0, str1;
        if (index == 1) {
            str0 = target.text();
            str1 = target1.text();
        } else {
            str0 = target1.text();
            str1 = target.text();
        }
        //
        var oddsObj = trObj.find(".oddsItem");
        trObj.find(".oddsClk").click();

        if (str0.indexOf("场") != -1 || str1.indexOf("场") != -1) {
            return;
        }

        for (var i = 0; i < str0.length; i++) {
            for (var j = 0; j < str1.length; j++) {
                var curOddsObj = oddsObj.eq(Number(oddsName[str0.charAt(i) + "" + str1.charAt(j)]));
                curOddsObj.click();
            }
        }

    });
});
function selItemByHFSel() {

}
function getData(backData) {
    if (backData == undefined || backData == "" || backData.data == undefined || backData.data == "" || (backData.data.length != undefined && backData.data.length == 0)) {
        //alert("没有比赛数据！");
        $("#mainTbl").html(noMatchShow);
        return;
    }
    lotFunc.updateTime(backData.status.last_updated);
    var newDataFlag = Boolean(curData.length);
    if (!newDataFlag) {
        var bDate = "";
        for (var key in backData.data) {
            var tmpObj = backData.data[key];
            if (tmpObj.b_date != bDate) {
                curData.push([]);
                bDate = tmpObj.b_date;
            }
            curData[curData.length - 1].push(tmpObj);
        }
    }
    initData();
}
function initData() {
    if (curData.length == 0) {
        return;
    }
    filteAry = { date: [], league: [] };
    var htmlStr = "";
    for (var i = 0; i < curData.length; i++) {
        var tmpDate = curData[i][0].b_date;
        var weekObj = lotFunc.getWeek(tmpDate);
        filteAry.date.push(weekObj.cn);
        var matchNumDate = curData[i][0].match_num_date
        htmlStr += "<tr><td class='bDateTd' colspan='8' bIndex='" + filteAry.date.length + "'>周" + weekObj.cn + " " + tmpDate + " 共<label></label>场比赛 "
        if(matchNumDate !='' && matchNumDate != undefined){
            htmlStr += '<span class="match-date-num">(比赛编号日期：'+ matchNumDate +")</span>"
        }
        htmlStr +="<a href='javascript:void(0);' class='bDateHide'>[隐藏]</a>"
        htmlStr +="</td></tr>";
        for (var j = 0; j < curData[i].length; j++) {
            var tmpObj = curData[i][j];

            var isFind = -1;
            for (var m = 0; m < filteAry.league.length; m++) {
                if (filteAry.league[m] == tmpObj.l_cn) {
                    isFind = m + 1;
                    break;
                }
            }
            if (isFind == -1) {
                filteAry.league.push(tmpObj.l_cn);
                isFind = filteAry.league.length;
            }
            var bgStr = "";
            var tmpNum = Math.round(Math.random() * 10); //测试数据 || tmpNum % 7 == 0
            if ((tmpObj.hafu.single == "1" && tmpObj.hafu.o_type == "F")) {
                bgStr = "background-image:url("+jsCommonDataV1.resDomain+"/res_1_0/jcw/images/jc_calculator/single.gif);background-repeat:no-repeat;background-position:left top;";
            }
            var disCls = "";
            if (tmpObj.hafu != undefined && tmpObj.hafu.cbt == "2") disCls = " oddsDis";
            const rowAttrs = {
                id: `list_${tmpObj.id}`,
                dataIndex: `${i}_${j}`,
                lIndex: isFind,
                bIndex: filteAry.date.length,
                matchNumDate: tmpObj.match_num_date,
                matchNum: tmpObj.match_num,
                taxDateNo: tmpObj.tax_date_no
            };

            const numDisplay = `${tmpObj.num.slice(0, 2)}<br>${tmpObj.num.slice(2)}`;
            const dateDisplay = `${tmpObj.date.slice(5)}<br>${tmpObj.time.slice(0, 5)}`;
            const targetHtml = getTarget(tmpObj.id, tmpObj.l_cn_abbr, tmpObj.l_id, tmpObj.l_background_color);

            // 条件样式提取
            const isFirstRow = i === 0;
            const tdStyle = isFirstRow ? 'width:43px;' : '';
            const vsTdStyle = isFirstRow ? 'width:350px;' : '';
            const oddsTdStyle = isFirstRow ? 'width:480px;' : '';

            // 图片资源提取
            const subscribeTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
            const homeTeamImg = `<img  title=""  src="${subscribeTeamImgUrl}" class="subscribe-icon img_${tmpObj.h_id}" onclick="stopEvent(event);bqcGz.subscribeTeam('${tmpObj.h_id}', '${tmpObj.h_cn_abbr}', true, '${tmpObj.l_id}')">`;
            const awayTeamImg = `<img  title=""  src="${subscribeTeamImgUrl}" class="subscribe-icon img_${tmpObj.a_id}" onclick="stopEvent(event);bqcGz.subscribeTeam('${tmpObj.a_id}', '${tmpObj.a_cn_abbr}', false, '${tmpObj.l_id}')">`;
            const matchSubscribeImg = `<img src="https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png" id="img_${tmpObj.id}" class="subscribe-icon-sc img_${tmpObj.id}" style="cursor: pointer;" onclick="stopEvent(event);bqcGz.subscribeMatch('${tmpObj.l_id}', '${tmpObj.id}', '${tmpObj.match_num}')">`;

            // 赔率数据映射生成（自动处理最后一项的 border-right 样式）
            const hafuHtml = tmpObj.hafu.hh
                ? [tmpObj.hafu.hh, tmpObj.hafu.hd, tmpObj.hafu.ha, tmpObj.hafu.dh, tmpObj.hafu.dd, tmpObj.hafu.da, tmpObj.hafu.ah, tmpObj.hafu.ad, tmpObj.hafu.aa]
                    .map((val, idx) => {
                        const extraStyle = idx === 8 ? " style='border-right:none;'" : '';
                        return `<span style="width: 50px" class='oddsItem oddsItemKeep${disCls}'${extraStyle}>${val}</span>`;
                    })
                    .join('')
                : '';

            htmlStr += `
                <tr id="${rowAttrs.id}" dataIndex="${rowAttrs.dataIndex}" class="listTr" lIndex="${rowAttrs.lIndex}" bIndex="${rowAttrs.bIndex}" matchNumDate="${rowAttrs.matchNumDate}" taxDateNo="${rowAttrs.taxDateNo}">
                  <td style="${tdStyle}">${numDisplay}</td>
                  <td style="width:70px" title="${tmpObj.l_cn}">${targetHtml}</td>
                  <td style="width:50px">${dateDisplay}</td>
                  <td title="点击VS进入对阵数据页,点击球队名称进入球队介绍页" onclick="toArrData(${tmpObj.id})" class="vsTd" style="${vsTdStyle}${bgStr};width:370px">
                    <span class='match-left' style="width: 164px">${homeTeamImg}<a onclick="stopEvent(event)" href="//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid=${tmpObj.h_id}" target="_blank">`
            if(tmpObj.h_order !=''){
                htmlStr += `<label>${tmpObj.h_order}</label>`
            }
            htmlStr += `<span class="vs-left-padding">${tmpObj.h_cn_abbr}</span></a>
            </span>
                    <a onclick="stopEvent(event)" class="vsA" style='width:19px' target="_blank" href="//www.sporttery.cn/jc/zqdz/index.html?showType=2&mid=${tmpObj.id}"> VS </a>
                    <span class='match-right' style="width: 163px">
                    <a onclick="stopEvent(event)" href="//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid=${tmpObj.a_id}" target="_blank"><span class="vs-right-padding">${tmpObj.a_cn_abbr}</span>`
            if(tmpObj.a_order !=''){
                htmlStr +=`<label>${tmpObj.a_order}</label>`}
            htmlStr +=`</a>${awayTeamImg}`
            htmlStr +=`</span></td>
                  <td class="hafuOdds" style="${oddsTdStyle};width: 482px">${hafuHtml}</td>
                  <td class="selTd" style="width: 54px">
                    <a href="javascript:void(0)" class="hafuSel" txt="半场">半场</a>
                    <a href="javascript:void(0)" class="hafuSel" txt="全场">全场</a>
                  </td>
                  <td class="uOddsTd" style="width: 118px;">
                    <div class="hafuU">
                      <span class="rLine" style="width: 36px;">--</span><span class="rLine" style="width: 36px;">--</span><span style="width: 36px;">--</span>
                    </div>
                  </td>
                  <td style="width: 46px;">${matchSubscribeImg}</td>
                </tr>
              `;
        }
    }
    $("#mainTbl").html(htmlStr);
    //
    $("#mainTbl span.oddsItem").each(function() {
        if ($(this).text() == "") $(this).text("--");
    });
    //
    lotFunc.initOK();
    //
    var hTdObj = $("#headerTr td");
    var mTdObj = $("#mainTbl tr:eq(1) td");
    for (var i = 0; i < hTdObj.length; i++) {
        hTdObj.eq(i).width(mTdObj.eq(i).width());
    }
    //
    if (!loadedOk) {
        if (listTop < 0) listTop = $("#headerTr").offset().top;
        $(window).scroll(lotFunc.scrollFunc);
        setTimeout(lotFunc.scrollFunc, 500);
        loadedOk = true;
    }
    //
    lotFunc.getReferDataI(0);
    bqcGz.showHtml = true
}
function setAnn(anns) {
    lotFunc.setAnn(anns);
}
function updateSelDetail() {
    if ($("#selDetailDiv").css("display") == "none") return;
    var str = "<tr style='font-weight:bold;'><td><a href='javascript:void(0)' id='clearSel'>清空</a></td><td>编号</td><td>对阵</td><td>投注选项</td><td>胆<label id='selDetailClose'>×</label></td></tr>";
    var poolStr = "";
    if (selAryLen > 0) {
        for (var key in selAry) {
            var selObj = selAry[key];
            var indexAry = selObj.dataIndex.split("_");
            var tmpAry = selObj.odds;
            var dataObj = curData[indexAry[0]][indexAry[1]];
            var selItemStr = "";
            for (var i = 0; i < tmpAry.length; i++) {
                if (tmpAry[i] != "") {
                    selItemStr += "<span title='" + tmpAry[i] + "' index='" + i + "'>" + oddsIndex[i] + "</span>";
                }
            }
            str += "<tr id='sel_" + key + "'><td class='delSelLine'>×</td><td>" + dataObj.num + "</td><td>" + dataObj.h_cn_abbr + " VS " + dataObj.a_cn_abbr + "</td><td class='selItemTd'>" + selItemStr + "</td><td><input" + (($("#optionHeader input").attr("disabled") != undefined) ? " disabled" : "") + " type='checkbox'" + ((selObj.isDan) ? " checked" : "") + " class='danChk' /></td></tr>";
        }
    }
    $("#selDetailTbl").html(str);
    lotFunc.autoScroll();
    lotFunc.danCheck();
}
function getCopyData(param) {
    var str = getFixedLength(10, "赛事编号") + getFixedLength(10, "赛事") + getFixedLength(16, "开赛时间") + getFixedLength(24, "对阵") + getFixedLength(10, "胜胜") + getFixedLength(10, "胜平") + getFixedLength(10, "胜负") + getFixedLength(10, "平胜") + getFixedLength(10, "平平") + getFixedLength(10, "平负") + getFixedLength(10, "负胜") + getFixedLength(10, "负平") + getFixedLength(10, "负负") + "\r\n";
    $("#mainTbl tr.listTr:visible").each(function() {
        var tdObj = $(this).find("td");
        var oddsObj = tdObj.eq(4).find("span");
        str += getFixedLength(10, tdObj.eq(0).text()) + getFixedLength(10, tdObj.eq(1).text()) + getFixedLength(16, tdObj.eq(2).html().toLocaleLowerCase().replace("<br>", " ")) + getFixedLength(24, tdObj.eq(3).text()) + getFixedLength(10, oddsObj.eq(0).text()) + getFixedLength(10, oddsObj.eq(1).text()) + getFixedLength(10, oddsObj.eq(2).text()) + getFixedLength(10, oddsObj.eq(3).text()) + getFixedLength(10, oddsObj.eq(4).text()) + getFixedLength(10, oddsObj.eq(5).text()) + getFixedLength(10, oddsObj.eq(6).text()) + getFixedLength(10, oddsObj.eq(7).text()) + getFixedLength(10, oddsObj.eq(8).text()) + "\r\n";
    });
    return str;
}

var bqcGz = {
    showHtml: false,
    canLoginTag: false,
    canOrderTag: false,
    teamList: [],
    matchList: [],
    refreshDebounceTimer: null,
    subscribeTeam: async function(teamId, teamName, leagueId) {
        if(!bqcGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if ( bqcGz.refreshDebounceTimer) {
            return;
        }
        bqcGz.refreshDebounceTimer = setTimeout(function() {
            bqcGz.refreshDebounceTimer = null;
        }, 1500);
        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
        var imgSelector = '.subscribe-icon.img_' + teamId;

        try {
            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            let objTrack = {
                teamId:teamId,
                leagueId: leagueId,
                status:isSubscribe?1:0
            }
            bqcGz.setTrackEvent1('click_jingcaiFollowTeam_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await teamSubscribeSubmitFun(bqcGz.scanChannel, '0', teamId, teamName, 0);
                console.log('subscribeTeam 结果:', result);
                if(result.errCode == 0) {
                    bqcGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bqcGz.initSubscribeStatus(bqcGz.teamList, 2);
                    bqcGz.initSubscribeMatchStatus(bqcGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bqcGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await teamSubscribeCancelFun('0', teamId, 0);
                console.log('subscribeTeamCancel 结果:', result);
                if(result.errCode == 0) {
                    bqcGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bqcGz.initSubscribeStatus(bqcGz.teamList, 2);
                    bqcGz.initSubscribeMatchStatus(bqcGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bqcGz.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeTeam 错误:', error);
        }
    },
    refreshDebounceTimer2: null,
    subscribeMatch: async function(leagueId, matchId, matchNum) {
        if(!bqcGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if ( bqcGz.refreshDebounceTimer2) {
            return;
        }
        bqcGz.refreshDebounceTimer2 = setTimeout(function() {
            bqcGz.refreshDebounceTimer2 = null;
        }, 1500);

        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
        var imgSelector = '.subscribe-icon-sc.img_' + matchId;

        try {
            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            let objTrack ={
                leagueId:leagueId,
                matchId:matchId,
                matchNum:matchNum,
                status:isSubscribe?1:0
            }
            bqcGz.setTrackEvent1('click_jingcaiFollowEvent_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await matchSubscribeSubmitFun(bqcGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatch 结果:', result);
                if(result.errCode == 0) {
                    bqcGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bqcGz.initSubscribeStatus(bqcGz.teamList, 2);
                    bqcGz.initSubscribeMatchStatus(bqcGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bqcGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await matchSubscribeCancelFun(bqcGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatchCancel 结果:', result);
                if(result.errCode == 0) {
                    bqcGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bqcGz.initSubscribeStatus(bqcGz.teamList, 2);
                    bqcGz.initSubscribeMatchStatus(bqcGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bqcGz.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeMatch 错误:', error);
        }
    },
    initSubscribeStatus: function(subscribedTeamIds,t) {
        if(t == 1) {
            var subscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.subscribe-icon.img_' + teamId).attr('src', subscribedTeamImgUrl);
                    $('.subscribe-icon.img_' + teamId).show()
                }
            }
            bqcGz.canOrderTag = true
        } else {
            var subscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.subscribe-icon.img_' + teamId).attr('src', subscribedTeamImgUrl);
                }
            }
        }

    },
    initSubscribeMatchStatus: function(subscribedMatchIds,t) {
        if(t ==1){
            // 选中
            var subscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
            $('.subscribe-icon-sc.img_').not('[src*="scgz.png"]').hide();
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', subscribedMatchImgUrl).show();
                }
            }
        } else {
            var subscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', subscribedMatchImgUrl);
                }
            }
        }


    },

    loadSubscribedTeams: async function() {
        if(!bqcGz.canLoginTag) {
            return
        }
        try {
            const result = await getSubscribeMatchListFun(
                bqcGz.scanChannel,
                '0',
                'current'
            );
            bqcGz.matchList = [];
            bqcGz.teamList = [];
            console.log('getSubscribeMatchListFun 结果:', result);
            if(result.errCode == 0) {
                if(result.data.matchList && result.data.matchList.length > 0){
                    result.data.matchList.forEach((element, index) => {
                        bqcGz.matchList.push(element.matchId)
                    })
                }
                if(result.data.teamList && result.data.teamList.length > 0){
                    result.data.teamList.forEach((element, index) => {
                        bqcGz.teamList.push(element.teamId)
                    })
                }
                bqcGz.initSubscribeStatus(bqcGz.teamList, 1);
                bqcGz.initSubscribeMatchStatus(bqcGz.matchList, 1);
            }
        } catch (error) {
            console.error('getSubscribeMatchListFun 错误:', error);
        }
    },
    showToast: function(message, duration) {
        duration = duration || 1500;

        var toastId = 'bqc_gz_toast_' + Date.now();
        var toastHtml = '<div id="' + toastId + '" style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0, 0, 0, 0.75); color: #fff; padding: 16px 24px; border-radius: 8px; font-size: 14px; z-index: 9999; text-align: center; min-width: 160px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);">' + message + '</div>';

        $('body').append(toastHtml);

        setTimeout(function() {
            $('#' + toastId).fadeOut(300, function() {
                $(this).remove();
            });
        }, duration);
    },
    setTrackEvent1:function(eventName, data={}){
        try{
            dc.trackEvent(eventName, data)
        }catch(error){
            console.log(error.message)
        }
    }
};

$(document).ready(function() {
    bqcGz.loadSubscribedTeams();
});

function callListdata() {
    bqcGz.scanChannel = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1;
    bqcGz.scanTokenTag = window.sessionStorage.getItem('getScanToken') || '';
    bqcGz.canLoginTag = true;

    if(bqcGz.showHtml){
        bqcGz.loadSubscribedTeams();
    } else {
        const checkBfGz = setInterval(() => {
            if (bqcGz.showHtml) {
                clearInterval(checkBfGz);
                bqcGz.loadSubscribedTeams();
            }
        }, 100);
    }
}
function stopEvent(e){ //阻止冒泡事件
    //取消事件冒泡
    var theEvent = window.event || e;
    if (theEvent && theEvent.stopPropagation) {
        theEvent.stopPropagation();
    } else if (window.event) {
        window.event.cancelBubble = true;
    }
}

