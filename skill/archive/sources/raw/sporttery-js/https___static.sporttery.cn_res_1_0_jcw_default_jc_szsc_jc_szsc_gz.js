var jcSzsc = {
    canLoginTag : false,
    canOrderTag : false,
    scanTokenTag: '',
    scanChannel:'',
    teamList: [],
    matchList: [],
    obj_match_info: [],
    noMatchs: '<div class="m-czNo"><img src="' + jsCommonDataV1.resDomain + '/res_1_0/jcw/images/ico_game_no.png"></div>',
    channelInfo: {
        type: 'football',
        page: ''
    },
    getData: function () {
        jcSzsc.getChannelType();
        var apiUrl = '/gateway/uniform/' + jcSzsc.channelInfo.type + '/getMatchListV1.qry?clientCode='+commonV1Fun.comClientCode;
        if(jcSzsc.channelInfo.type == "basketball"){
            apiUrl = '/gateway/uniform/' + jcSzsc.channelInfo.type + '/getMatchListV2.qry?clientCode='+commonV1Fun.comClientCode;
        }


        commonV1Fun.ajaxFun(
            jcSzsc.showData,
            jsCommonDataV1.webApi + apiUrl,
            undefined,
            'get'
        )
    },
    showData: function (d) {
        var leagueList = [];
        var matchInfoList = [];
        var matchDateList = [];
        var lastUpdateTime = '';
        var totalCount = 0;
        if (d.errorCode == 0) {
            if (Object.keys(d.value).length != 0) {
                if (d.value.leagueList) {
                    leagueList = d.value.leagueList;
                }
                if (d.value.matchDateList) {
                    matchDateList = d.value.matchDateList;
                }
                if (d.value.matchInfoList) {
                    matchInfoList = d.value.matchInfoList;
                }
                lastUpdateTime = d.value.lastUpdateTime;
                totalCount = d.value.totalCount;
                if (leagueList.length > 0) {
                    $("#leagueList").html(jcSzsc.getLeagueList(leagueList));
                }
                if (matchDateList.length > 0) {
                    $("#dFilterList").html(jcSzsc.getWeekList(matchDateList));
                }
                if (matchInfoList.length > 0) {
                    if (jcSzsc.channelInfo.type == 'basketball') {
                        if (jcSzsc.channelInfo.page == 'dy') {
                            $("#matchList").html(jcSzsc.getMatchListBKP(matchInfoList));
                        } else {
                            $("#matchList").html(jcSzsc.getMatchListBK(matchInfoList));
                        }
                    } else {
                        if (jcSzsc.channelInfo.page == 'dy') {
                            $("#matchList").html(jcSzsc.getMatchListP(matchInfoList));
                        } else {
                            $("#matchList").html(jcSzsc.getMatchList(matchInfoList));
                        }
                    }
                } else {
                    $("#matchList").html(jcSzsc.noMatchs);
                }
                jcSzsc.getTableTitleInfo(totalCount, lastUpdateTime);
            } else {
                $("#matchList").html(jcSzsc.noMatchs);
            }
        } else {
            $("#matchList").html(jcSzsc.noMatchs);
        }
        $("input[name='week']").prop("checked", true);
        $("input[name='leagues']").prop("checked", true);
        $("#check_week").prop("checked", true);
        $("#check_leagues").prop("checked", true);
        updateTableCss();
    },


    getMatchList: function (matchInfoList) {
        var str = '';
        for (var i = 0; i < matchInfoList.length; i++) {
            var posIndex = i;
            var weekListMatch = matchInfoList[i];
            str += '<div class="week">' + weekListMatch.weekday
            if(weekListMatch.businessDate !='' && weekListMatch.businessDate != undefined){
                str += ' '+ weekListMatch.businessDate
            }
            str +=' 共<label>' + weekListMatch.matchCount + '</label>场比赛 '
            if(weekListMatch.matchNumDate !='' && weekListMatch.matchNumDate != undefined){
                str +='<span class="week-num">(比赛编号日期：'+ weekListMatch.matchNumDate +
                    ')</span>'
            }
            str +='<a href="javascript:void(0);" class="bDateHide" onclick="return false;" weekindex=' + posIndex + '>隐藏</a>'

            str +='</div>';
            str += '<table width="100%" border="0" cellpadding="0" cellspacing="0" class="m-tab week' + posIndex + '">';

            for (var j = 0; j < weekListMatch.subMatchList.length; j++) {
                var matchInfo = weekListMatch.subMatchList[j];
                str += '<tr windex="' + posIndex + '" lindex="' + matchInfo.leagueId + '">' +
                    '<td width="64">' + matchInfo.matchNumStr + '</td>';

                // 修复 1: 添加属性转义函数，防止 XSS
                var safeLeagueAllName = this.escapeHtml(matchInfo.leagueAllName || '');
                var safeBackColor = this.escapeHtml(matchInfo.backColor || '');
                var safeLeagueAbbName = this.escapeHtml(matchInfo.leagueAbbName || '');

                var safeHomeTeamId = this.escapeHtml(matchInfo.homeTeamId || '');
                var safeAwayTeamId = this.escapeHtml(matchInfo.awayTeamId || '');
                var safeHomeTeamAllName = this.escapeHtml(matchInfo.homeTeamAllName || '');
                var safeAwayTeamAllName = this.escapeHtml(matchInfo.awayTeamAllName || '');
                var safeMatchDate = this.escapeHtml(matchInfo.matchDate || '');
                var safeMatchTime = this.escapeHtml(matchInfo.matchTime || '');
                var safeMatchLeagueId = this.escapeHtml(matchInfo.leagueId || '');
                var subscribeImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';

                var leagueCell = '<td width="77" class="write" title="' + safeLeagueAllName + '">' +
                    '<div class="league-bg" style="background: #' + safeBackColor + '">' + safeLeagueAbbName + '</div>' +
                    '</td>';

                var homeSubscribeImg = '<img src="' + subscribeImgUrl + '" ' +
                    'id="img_' + safeHomeTeamId + '" ' +
                    'style="margin-right: 5px; vertical-align: text-top; cursor: pointer;" ' +
                    'class="subscribe-icon img_' + safeHomeTeamId + '" ' +
                    'onclick="jcSzsc.subscribeTeam(\'' + safeHomeTeamId + '\', \'' + safeHomeTeamAllName + '\', true, \'' + safeMatchLeagueId + '\')" />';

                var awaySubscribeImg = '<img src="' + subscribeImgUrl + '" ' +
                    'id="img_' + safeAwayTeamId + '" ' +
                    'style="margin-left: 5px; vertical-align: text-top; cursor: pointer;" ' +
                    'class="subscribe-icon img_' + safeAwayTeamId + '" ' +
                    'onclick="jcSzsc.subscribeTeam(\'' + safeAwayTeamId + '\', \'' + safeAwayTeamAllName + '\', false, \'' + safeMatchLeagueId + '\')" />';

                var matchLink = '<a href="https://www.sporttery.cn/jc/zqdz/index.html?showType=2&amp;mid=' + matchInfo.matchId + '" target="_blank">' +
                    '<span class="zhu_gz">' + safeHomeTeamAllName + '</span>' +
                    '<span class="vs_gz">VS</span>' +
                    '<span class="ke_gz">' + safeAwayTeamAllName + '</span>' +
                    '</a>';

                var matchCell = '<td width="429">' + homeSubscribeImg + matchLink + awaySubscribeImg + '</td>';

                var timeCell = '<td width="126">' + safeMatchDate + ' ' + safeMatchTime + '</td>';

                var analysisCell = '<td width="66" class="blue">' +
                    '<a target="_blank" href="https://www.sporttery.cn/jc/zqdz/index.html?showType=2&mid=' + matchInfo.matchId + '">析</a>&nbsp;' +
                    '<a target="_blank" href="https://www.sporttery.cn/jc/zqdz/index.html?showType=1&mid=' + matchInfo.matchId + '">讯</a>' +
                    '</td>';

                str += leagueCell + matchCell + timeCell + analysisCell;
                var sellStatus = '';
                var had = hhad = ttg = hafu = crs = 'class="u-kong"';
                if (matchInfo.sellStatus == "1") {
                    sellStatus = '已开售';
                    matchInfo.poolList.forEach(function (val, idx) {
                        switch (val.poolCode) {
                            case 'HAD':
                                had = jcSzsc.getSingleStatus(val);
                                break;
                            case 'HHAD':
                                hhad = jcSzsc.getSingleStatus(val);
                                break;
                            case 'TTG':
                                ttg = jcSzsc.getSingleStatus(val);
                                break;
                            case 'HAFU':
                                hafu = jcSzsc.getSingleStatus(val);
                                break;
                            case 'CRS':
                                crs = jcSzsc.getSingleStatus(val);
                                break;
                        }

                    })
                } else if(matchInfo.sellStatus == "2"){
                    sellStatus = '暂停销售';
                    had = hhad = ttg = hafu = crs = '';
                }else{
                    sellStatus = '待开售';
                    had = hhad = ttg = hafu = crs = '';
                }
                str += '<td width="86">' + sellStatus + '</td>';
                str += '<td width="45"><a href="/jc/jsq/zqspf/" target="_blank"><div ' + had + '></div></a></td>';
                str += '<td width="36"><a href="/jc/jsq/zqspf/" target="_blank"><div ' + hhad + '></div></a></td>';
                str += '<td width="45"><a href="/jc/jsq/zqbf/" target="_blank"><div ' + crs + '></div></a></td>';
                str += '<td width="45"><a href="/jc/jsq/zqzjq/" target="_blank"><div ' + ttg + '></div></a></td>';
                str += '<td width="45"><a href="/jc/jsq/zqbqc/" target="_blank"><div ' + hafu + '></div></a></td>';
                str += '<td width="46">'
                if (matchInfo.remark != '') {
                    str += '<div id="minfo_' + matchInfo.matchId + '" class="prompt" style="background: url(\'https://static.sporttery.cn/res_1_0/jcw/upload/202209/fb_alert.png\') no-repeat;"></div>';
                    this.obj_match_info[matchInfo.matchId] = matchInfo.remark;
                } else {
                    str += '&nbsp;';
                }
                var subscribeImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
                str += '</td><td width="46"><img src="' + subscribeImgUrl + '" id="img_' + matchInfo.matchId + '" class="subscribe-icon-sc img_' + matchInfo.matchId + '" style="cursor: pointer;" onclick="jcSzsc.subscribeMatch(\'' + matchInfo.leagueId + '\', \'' + matchInfo.matchId + '\', \'' + matchInfo.matchNum + '\')"></td></tr>';
            }
            str += '</table>'
        }
        return str;
    },
    // 新增：HTML 转义函数，防止 XSS 攻击
    escapeHtml: function(str) {
        if (!str) return '';
        var map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        };
        return String(str).replace(/[&<>"']/g, function(m) { return map[m]; });
    },
    getMatchListP: function (matchInfoList) {
        var str = '';
        for (var i = 0; i < matchInfoList.length; i++) {
            var posIndex = i;
            var weekListMatch = matchInfoList[i];
            str += '<div class="week">' + weekListMatch.weekday
            if(weekListMatch.businessDate !='' && weekListMatch.businessDate != undefined){
                str += ' '+ weekListMatch.businessDate
            }
            str +=' 共<label>' + weekListMatch.matchCount + '</label>场比赛 '
            if(weekListMatch.matchNumDate !='' && weekListMatch.matchNumDate != undefined){
                str +='<span class="week-num">(比赛编号日期：'+ weekListMatch.matchNumDate +')</span>'
            }
            str +='</div>';
            str += '<table width="100%" border="0" cellpadding="0" cellspacing="0" class="m-tab week' + posIndex + '">';

            for (var j = 0; j < weekListMatch.subMatchList.length; j++) {
                var matchInfo = weekListMatch.subMatchList[j];
                str += '<tr windex="' + posIndex + '" lindex="' + matchInfo.leagueId + '">' +
                    '<td width="64">'+ matchInfo.matchNumStr + '</td>';
                str += '<td width="112" class="td_ls" bgcolor="' + matchInfo.backColor + '" title="' + matchInfo.leagueAllName + '">' + matchInfo.leagueAllName + '</td>' +
                    '<td width="300"><span class="zhu">' + matchInfo.homeTeamAllName + '</span><span class="vs">VS</span><span class="ke">' + matchInfo.awayTeamAllName + '</span></td>' +
                    '<td width="115">' + matchInfo.matchDate + " " + matchInfo.matchTime + '</td>'
                var sellStatus = '';
                var had = hhad = ttg = hafu = crs = 'class="u-kong"';
                if (matchInfo.sellStatus == "1") {
                    sellStatus = '已开售';
                    matchInfo.poolList.forEach(function (val, idx) {
                        switch (val.poolCode) {
                            case 'HAD':
                                had = jcSzsc.getSingleStatus(val);
                                break;
                            case 'HHAD':
                                hhad = jcSzsc.getSingleStatus(val);
                                break;
                            case 'TTG':
                                ttg = jcSzsc.getSingleStatus(val);
                                break;
                            case 'HAFU':
                                hafu = jcSzsc.getSingleStatus(val);
                                break;
                            case 'CRS':
                                crs = jcSzsc.getSingleStatus(val);
                                break;
                        }

                    })
                } else if(matchInfo.sellStatus == "2"){
                    sellStatus = '暂停销售';
                    had = hhad = ttg = hafu = crs = '';
                }else{
                    sellStatus = '待开售';
                    had = hhad = ttg = hafu = crs = '';
                }
                str += '<td width="54">' + sellStatus + '</td>';
                str += '<td width="44"><div ' + had + '></div></td>';
                str += '<td width="44" class="l13"><div ' + hhad + '></div></td>';
                str += '<td width="44"><div ' + crs + '></div></td>';
                str += '<td width="44" class="l13"><div ' + ttg + '></div></td>';
                str += '<td width="44" class="l13"><div ' + hafu + '></div></td>';
                str += '<td width="58">'
                if (matchInfo.remark != '') {
                   str += '<div id="minfo_' + matchInfo.matchId + '" class="prompt" style="background: url(\'https://static.sporttery.cn/res_1_0/jcw/upload/202209/fb_alert.png\') no-repeat;"></div>';
                    this.obj_match_info[matchInfo.matchId] = matchInfo.remark;
                } else {
                    str += '&nbsp;';
                }
                str += '</td></tr>';
            }
            str += '</table>'
        }
        return str;
    },
    getMatchListBK: function (matchInfoList) {
        var str = '';
        if(matchInfoList && matchInfoList.length>0){
            for (var i = 0; i < matchInfoList.length; i++) {
                var posIndex = i;
                var weekListMatch = matchInfoList[i];
                str += '<div class="week">' + weekListMatch.weekday
                if(weekListMatch.businessDate !='' && weekListMatch.businessDate != undefined){
                    str += ' '+ weekListMatch.businessDate
                }
                str +=' 共<label>' + weekListMatch.matchCount + '</label>场比赛 '
                if(weekListMatch.matchNumDate !='' && weekListMatch.matchNumDate != undefined){
                    str +='<span class="week-num">(比赛编号日期：'+ weekListMatch.matchNumDate +')</span>'
                }
                str +='<a href="javascript:void(0);" class="bDateHide" onclick="return false;" weekindex=' + posIndex + '>隐藏</a></div>';
                str += '<table width="100%" border="0" cellpadding="0" cellspacing="0" class="m-tab week' + posIndex + '">';
                if(weekListMatch.subMatchList && weekListMatch.subMatchList.length>0){
                    for (var j = 0; j < weekListMatch.subMatchList.length; j++) {
                        var matchInfo = weekListMatch.subMatchList[j];
                        str += '<tr windex="' + posIndex + '" lindex="' + matchInfo.leagueId + '">' +
                            '<td width="64">' + matchInfo.matchNumStr + '</td>' +
                            '<td width="77" title="' + matchInfo.leagueAllName + '">' + matchInfo.leagueAbbName + '</td>' +
                            '<td width="385"><a href="https://www.sporttery.cn/jc/lqdz/index.html?showType=2&mid=' + matchInfo.matchId + '" target="_blank"><span class="zhu">' + matchInfo.awayTeamAllName + '</span><span class="vs">VS</span><span class="ke">' + matchInfo.homeTeamAllName + '</span></a></td>' +
                            '<td width="157">' + matchInfo.matchDate + " " + matchInfo.matchTime + '</td>' +
                            '<td width="93" class="blue">' +
                            '<a target="_blank" href="https://www.sporttery.cn/jc/lqdz/index.html?showType=2&mid=' + matchInfo.matchId + '">析</a>&nbsp;' +
                            '<a target="_blank" href="https://www.sporttery.cn/jc/lqdz/?showType=1&mid=' + matchInfo.matchId + '">讯</a>' +
                            '</td>';
                        var sellStatus = '';
                        var mnl = hdc = wnm = hilo = 'class="u-kong"';
                        if (matchInfo.sellStatus == "1") {
                            sellStatus = '已开售';
                            matchInfo.poolList.forEach(function (val, idx) {
                                switch (val.poolCode) {
                                    case 'MNL':
                                        mnl = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'HDC':
                                        hdc = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'WNM':
                                        wnm = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'HILO':
                                        hilo = jcSzsc.getSingleStatus(val);
                                        break;
                                }

                            })
                        } else if(matchInfo.sellStatus == "2"){
                            sellStatus = '暂停销售';
                            mnl = hdc = wnm = hilo = '';
                        }else{
                            sellStatus = '待开售';
                            mnl = hdc = wnm = hilo = '';
                        }
                        str += '<td width="85">' + sellStatus + '</td>';
                        str += '<td width="55"><a href="/jc/jsq/lqsf/" target="_blank"><div ' + mnl + '></div></a></td>';
                        str += '<td width="55"><a href="/jc/jsq/lqrfsf/" target="_blank"><div ' + hdc + '></div></a></td>';
                        str += '<td width="55"><a href="/jc/jsq/lqdxf/" target="_blank"><div ' + hilo + '></div></a></td>';
                        str += '<td width="55"><a href="/jc/jsq/lqsfc/" target="_blank"><div ' + wnm + '></div></a></td>';
                        str += '<td width="87">'
                        if (matchInfo.remark != '') {
                          str += '<div id="minfo_' + matchInfo.matchId + '" class="prompt" style="background: url(\'https://static.sporttery.cn/res_1_0/jcw/upload/202209/bk_alert.png\') no-repeat;"></div>';
                            this.obj_match_info[matchInfo.matchId] = matchInfo.remark;
                        } else {
                            str += '&nbsp;';
                        }
                        str += '</td></tr>';
                    }
                }
                str += '</table>'
            }
        }
        return str;
    },
    getMatchListBKP: function (matchInfoList) {
        var str = '';
        if(matchInfoList && matchInfoList.length>0){
            for (var i = 0; i < matchInfoList.length; i++) {
                var posIndex = i;
                var weekListMatch = matchInfoList[i];
                str += '<div class="week">' + weekListMatch.weekday
                if(weekListMatch.businessDate !='' && weekListMatch.businessDate != undefined){
                    str += ' '+ weekListMatch.businessDate
                }
                str +=' 共<label>' + weekListMatch.matchCount + '</label>场比赛 '
                if(weekListMatch.matchNumDate !='' && weekListMatch.matchNumDate != undefined){
                    str +='<span class="week-num">(比赛编号日期：'+ weekListMatch.matchNumDate +')</span>'
                }
                str +='</div>';
                str += '<table width="100%" border="0" cellpadding="0" cellspacing="0" class="m-tab week' + posIndex + '">';
                if(weekListMatch.subMatchList && weekListMatch.subMatchList.length>0){
                    for (var j = 0; j < weekListMatch.subMatchList.length; j++) {
                        var matchInfo = weekListMatch.subMatchList[j];
                        str += '<tr windex="' + posIndex + '" lindex="' + matchInfo.leagueId + '">' +
                            '<td width="74">' + matchInfo.matchNumStr + '</td>' +
                            '<td width="142" title="' + matchInfo.leagueAllName + '">' + matchInfo.leagueAllName + '</td>' +
                            '<td width="305"><span class="zhu">' + matchInfo.awayTeamAllName + '</span><span class="vs">VS</span><span class="ke">' + matchInfo.homeTeamAllName + '</span></td>' +
                            '<td width="115">' + matchInfo.matchDate + " " + matchInfo.matchTime + '</td>';
                        var sellStatus = '';
                        var mnl = hdc = wnm = hilo = 'class="u-kong"';
                        if (matchInfo.sellStatus == "1") {
                            sellStatus = '已开售';
                            matchInfo.poolList.forEach(function (val, idx) {
                                switch (val.poolCode) {
                                    case 'MNL':
                                        mnl = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'HDC':
                                        hdc = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'WNM':
                                        wnm = jcSzsc.getSingleStatus(val);
                                        break;
                                    case 'HILO':
                                        hilo = jcSzsc.getSingleStatus(val);
                                        break;
                                }

                            })
                        } else if(matchInfo.sellStatus == "2"){
                            sellStatus = '暂停销售';
                            mnl = hdc = wnm = hilo = '';
                        }else{
                            sellStatus = '待开售';
                            mnl = hdc = wnm = hilo = '';
                        }
                        str += '<td width="57">' + sellStatus + '</td>';
                        str += '<td width="60"><div ' + mnl + '></div></td>';
                        str += '<td width="60"><div ' + hdc + '></div></td>';
                        str += '<td width="60"><div ' + hilo + '></div></td>';
                        str += '<td width="60"><div ' + wnm + '></div></td>';
                        str += '<td width="63">'
                        if (matchInfo.remark != '') {
                           str += '<div id="minfo_' + matchInfo.matchId + '" class="prompt" style="background: url(\'https://static.sporttery.cn/res_1_0/jcw/upload/202209/bk_alert.png\') no-repeat;"></div>';
                            this.obj_match_info[matchInfo.matchId] = matchInfo.remark;
                        } else {
                            str += '&nbsp;';
                        }
                        str += '</td></tr>';
                    }
                }
                str += '</table>'
            }
        }
        return str;
    },
    getLeagueList: function (data) {
        var str = '<input type="checkbox" checked="checked" id="check_leagues" onclick="selectAllLeagues()"> 全部<br>';
        for (var i = 0; i < data.length; i++) {
            var info = data[i]
            str += '<input type="checkbox" name="leagues" value="' + info.leagueId + '" title="' + info.leagueName + '"><span>' + info.leagueNameAbbr + '</span>';
        }
        return str;
    },
    getWeekList: function (data) {
        var str = '<input type="checkbox" checked="checked" onclick="selectAllWeek();" id="check_week"> 全部<br/>';
        for (var i = 0; i < data.length; i++) {
            var idx = i;
            var info = data[i]
            str += '<input type="checkbox" name="week" value="' + idx + '">' + info.businessDateCn;
        }
        return str;
    },
    getSingleStatus: function (val) {
        var css = '';
        if (val.cbtValue == 1) {
            if (val.cbtSingle == 1) {
                /*
                if (val.cbtAllUp == 1) {
                    css = 'class="u-dan"';
                } else {
                    css = 'class="u-cir"';
                }*/
                css = 'class="u-dan"';
            } else {
                if (val.cbtAllUp == 1) {
                    css = 'class="u-cir"';
                }
            }
        }
        return css;

    },
    getTableTitleInfo: function (count, time) {
        if (jcSzsc.channelInfo.type == 'basketball') {
            $("#links").html('<a href="/jc/lqszsc/" class="blue" target="_self">[刷新]</a><a class="blue" onclick="window.open(\'/jc/lqszscdy/\')" style="cursor:pointer;">[打印]</a>');
        } else {
            $("#links").html('<a href="/jc/zqszsc/" class="blue" target="_self">[刷新]</a><a class="blue" onclick="window.open(\'/jc/zqszscdy/\')" style="cursor:pointer;">[打印]</a>');
        }

        $(".u-org").html(count);
        $("#updateTime").html(time);
    },
    getChannelType: function () {
        var url = window.location.href;
        if (url.indexOf('/jc/lq') > 0) {
            jcSzsc.channelInfo.type = 'basketball';
        }
        if (url.indexOf('szscdy') > 0) {
            jcSzsc.channelInfo.page = 'dy';
        }
    },
    showPopover:function (obj){
        if($(obj).next().css('display') =="block"){
            $(obj).next().css('display', 'none')
        }else{
            $(obj).next().css('display', 'block')
        }
    },
    refreshDebounceTimer2: null,
    subscribeTeam: async function(teamId, teamName, isHomeTeam, leagueId) {
        if(!jcSzsc.canLoginTag ){
            showLoginModal()
            return
        }
        if ( jcSzsc.refreshDebounceTimer2) {
            return;
        }
        jcSzsc.refreshDebounceTimer2 = setTimeout(function() {
            jcSzsc.refreshDebounceTimer2 = null;
        }, 1500);
        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
        var imgSelector = '.img_' + teamId;
        try {

            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            // var getChan = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1
            let objTrack = {
                teamId:teamId,
                leagueId: leagueId,
                status:isSubscribe?1:0
            }
            jcSzsc.setTrackEvent1('click_jingcaiFollowTeam_button',{'ext1':JSON.stringify(objTrack)})
            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await teamSubscribeSubmitFun(
                    jcSzsc.scanChannel, '0', teamId, teamName, 0
                );
                console.log('subscribeTeam 结果:', result);
                if(result.errCode == 0) {
                    jcSzsc.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                }else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    jcSzsc.initSubscribeStatus(jcSzsc.teamList, 2);
                    jcSzsc.initSubscribeMatchStatus(jcSzsc.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    jcSzsc.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await teamSubscribeCancelFun(
                    '0', teamId, 0
                );
                console.log('subscribeTeamCancel 结果:', result);
                if(result.errCode == 0) {
                    jcSzsc.showToast('取消订阅关注成功');
                }else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    jcSzsc.initSubscribeStatus(jcSzsc.teamList, 2);
                    jcSzsc.initSubscribeMatchStatus(jcSzsc.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    jcSzsc.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('getSubscribeMatchListFun 错误:', error);
        }


    },
    initSubscribeStatus: function(subscribedTeamIds, t) {
        if (t === 1 || t === undefined) {
            var subscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.img_' + teamId).not('.subscribe-icon-sc').attr('src', subscribedTeamImgUrl);
                }
            }
            jcSzsc.canOrderTag = true
        } else {
            var unsubscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.img_' + teamId).not('.subscribe-icon-sc').attr('src', unsubscribedTeamImgUrl);
                }
            }
        }
    },
    initSubscribeMatchStatus: function(subscribedMatchIds, t) {
        if (t === 1 || t === undefined) {
            var subscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', subscribedMatchImgUrl);
                }
            }
        } else {
            var unsubscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', unsubscribedMatchImgUrl);
                }
            }
        }
    },
    refreshDebounceTimer1: null,
    subscribeMatch: async function(leagueId, matchId, matchNum) {
        if(!jcSzsc.canLoginTag ){
            showLoginModal()
            return
        }

        if ( jcSzsc.refreshDebounceTimer1) {
            return;
        }
        jcSzsc.refreshDebounceTimer1 = setTimeout(function() {
            jcSzsc.refreshDebounceTimer1 = null;
        }, 1500);
        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
        var imgSelector = '.subscribe-icon-sc.img_' + matchId;

        try {
            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            // var getChan = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1
            let objTrack ={
                leagueId:leagueId,
                matchId:matchId,
                matchNum:matchNum,
                status:isSubscribe?1:0
            }
            jcSzsc.setTrackEvent1('click_jingcaiFollowEvent_button',{'ext1':JSON.stringify(objTrack)})
            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await matchSubscribeSubmitFun(jcSzsc.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatch 结果:', result);
                if(result.errCode == 0) {
                    jcSzsc.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    jcSzsc.initSubscribeStatus(jcSzsc.teamList, 2);
                    jcSzsc.initSubscribeMatchStatus(jcSzsc.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    jcSzsc.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await matchSubscribeCancelFun(jcSzsc.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatchCancel 结果:', result);
                if(result.errCode == 0) {
                    jcSzsc.showToast('取消订阅关注成功');
                }else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    jcSzsc.initSubscribeStatus(jcSzsc.teamList, 2);
                    jcSzsc.initSubscribeMatchStatus(jcSzsc.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    jcSzsc.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeMatch 错误:', error);
        }
    },
    showToast: function(message, duration) {
        duration = duration || 1500;

        var toastId = 'jc_szsc_toast_' + Date.now();
        var toastHtml = '<div id="' + toastId + '" style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0, 0, 0, 0.75); color: #fff; padding: 16px 24px; border-radius: 8px; font-size: 14px; z-index: 9999; text-align: center; min-width: 160px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);">' + message + '</div>';

        $('body').append(toastHtml);

        setTimeout(function() {
            $('#' + toastId).fadeOut(300, function() {
                $(this).remove();
            });
        }, duration);
    },
    loadSubscribedTeams: async function() {
        if(!jcSzsc.canLoginTag) {
            return
        }
        try {
            jcSzsc.canOrderTag = false;
            const result = await getSubscribeMatchListFun(
                jcSzsc.scanChannel,
                '0',
                'current'
            );
            jcSzsc.matchList = [];
            jcSzsc.teamList = [];
            console.log('getSubscribeMatchListFun 结果:', result);
            if(result.errCode == 0) {
                if(result.data.matchList && result.data.matchList.length > 0){
                    result.data.matchList.forEach((element,index) => {
                        jcSzsc.matchList.push(element.matchId)
                    })
                }
                if(result.data.teamList && result.data.teamList.length > 0){
                    result.data.teamList.forEach((element,index) => {
                        jcSzsc.teamList.push(element.teamId)
                    })
                }
                jcSzsc.initSubscribeStatus(jcSzsc.teamList);
                jcSzsc.initSubscribeMatchStatus(jcSzsc.matchList);
            }else if(result.errCode == '-20132602'){
               goLoginFunPage(1)
            }
            jcSzsc.canOrderTag = true
        } catch (error) {
            jcSzsc.canOrderTag = true
            console.error('getSubscribeMatchListFun 错误:', error);
        }

    },
    setTrackEvent1(eventName, data={}){
        try{
            dc.trackEvent(eventName, data)
        }catch(error){
            console.log(error.message)
        }
    },

}
jcSzsc.getData();
$(document).ready(function () {
    $(document).on("mousemove", ".prompt", function (e) {
        var div_id = $(this).attr('id');
        var arr_div_id = div_id.split('_');

        //var arr_match_info = jcSzsc.obj_match_info[arr_div_id[1]];
        var str_match_info = jcSzsc.obj_match_info[arr_div_id[1]];
        /*
        var minfo_len = arr_match_info.length;
        for (var i = 0; i < minfo_len; i++) {
            if (str_match_info != '') {
                str_match_info += "\r\n";
            }
            str_match_info += arr_match_info[i];
        }
        */
        $('.poptip').html('<span class="poptip-arrow poptip-arrow-bottom"><em>◆</em><i>◆</i></span>' + str_match_info);
        var pop_tip = $('.poptip').height();
        var div_top = $(this).offset().top;
        var div_left = $(this).offset().left;
        $('.poptip').css({ 'top': div_top - pop_tip - 27, 'left': div_left - 84 });
        $('.poptip').css('display', 'block');
        e.stopPropagation();
    });
    $(document).on("mouseout", ".prompt", function (e) {
        $('.poptip').css('display', 'none');
        e.stopPropagation();
    });
});
// JavaScript Document
function selectAllWeek() {
    if ($("#check_week").prop("checked")) {
        $("input[name='week']").prop("checked", true);
        $("#check_leagues").prop("checked", true);
        $("input[name='leagues']").prop("checked", true);
        $("table.m-tab").show();
        $("table.m-tab tr").show();
        $(".bDateHide").html("隐藏");
    } else {
        $("input[name='week']").prop("checked", false);

        $("#check_leagues").prop("checked", false);
        $("input[name='leagues']").prop("checked", false);
        $("table.m-tab tr").hide();
        $(".bDateHide").html("显示");
    }
    updateMathchNumbers();
    updateTableCss();
}
//点击关注选项
function selectAllGz() {
    if ($("#check_gz").prop("checked")) {
        $("input[name='gzName']").prop("checked", true);
    } else {
        $("input[name='gzName']").prop("checked", false);
    }
}

$(document).on("click", "input[name='week']", function () {
    var tmpObj = $(this).closest("td");
    var windex = tmpObj.find("input").index($(this)) - 1;
    var tmpObj = $(".week" + windex);
    if ($(this).prop("checked")) {
        $(".week" + windex).show();
        $(".week" + windex + " tr[windex=" + windex + "]").show();
        $(".week" + windex).prev().find("a").html("隐藏");
    } else {
        $(".week" + windex + " tr[windex=" + windex + "]").hide();
        $(".week" + windex).prev().find("a").html("显示");
    }
    updateFilterMatch();
    setSelectAllFilter();
    updateMathchNumbers();
    updateTableCss();
})

function selectAllLeagues() {
    if ($("#check_leagues").prop("checked")) {
        $("input[name='leagues']").prop("checked", true);

        $("#check_week").prop("checked", true);
        $("input[name='week']").prop("checked", true);
        $("table.m-tab").show();
        $("table.m-tab tr").show();
        $(".bDateHide").html("隐藏");
    } else {
        $("input[name='leagues']").prop("checked", false);

        $("#check_week").prop("checked", false);
        $("input[name='week']").prop("checked", false);
        $("table.m-tab tr").hide();
        $(".bDateHide").html("显示");
    }
    updateMathchNumbers();
    updateTableCss();
}

$(document).on("click", "input[name='leagues']", function () {
    var tmpObj = $(this).closest("td");
    //var index = tmpObj.find("input").index($(this))-1;
    var index = $(this).val();
    if ($(this).prop("checked")) {
        $("table.m-tab tr[lindex=" + index + "]").show();

        $("table.m-tab").each(function (i, element) {
            var lindex = $(element).find("tr[lindex=" + index + "]").attr("lindex");
            var windex = $(element).find("tr[lindex=" + index + "]").attr("windex");
            if (lindex == index) {
                $("input[name='week']").eq(windex).prop("checked", true);
                $(".week" + windex).prev().find("a").html("隐藏");
                return;
            }

        })
    } else {
        $("table.m-tab tr[lindex=" + index + "]").hide();
        $("table.m-tab").each(function (i, element) {
            var len = $(element).find("tr:visible").length;
            if (len == 0) {
                var w = $(element).find("tr[lindex=\"" + index + "\"]").attr("windex");
                if (w != "undefined") {
                    $("input[name='week']").eq(w).prop("checked", false);
                    $(".week" + w).prev().find("a").html("显示");
                }

            }
        })
    }
    updateFilterMatch();
    setSelectAllFilter();
    updateMathchNumbers();
    updateTableCss();
})

$(document).on("click", ".bDateHide", function () {
    var weekindex = $(this).attr("weekindex");
    var obj = $(this).parent().next();
    if ($(this).html() == '显示') {
        obj.show();
        obj.find("tr").show();
        $(this).html("隐藏");
        $("input[name='week']").each(function (index) {
            if (index == weekindex) {
                $(this).prop("checked", true);
            }
        })
    } else {
        obj.css("display", "none");
        $(this).html("显示");
        obj.find("tr").hide();
        $("input[name='week']").each(function (index) {
            if (index == weekindex) {
                $(this).prop("checked", false);
            }
        })
    }
    updateFilterMatch();
    setSelectAllFilter();
    updateMathchNumbers();
    updateTableCss();
})

function setSelectAllFilter() {
    var chsub = $("input[name='week']").length; //获取subcheck的个数
    var checkedsub = $("input[name='week']:checked").length; //获取选中的subcheck的个数
    if (checkedsub == chsub) {
        $("#check_week").prop("checked", true);

    } else {
        $("#check_week").prop("checked", false);
    }
    var chsub1 = $("input[name='leagues']").length; //获取subcheck的个数
    var checkedsub1 = $("input[name='leagues']:checked").length; //获取选中的subcheck的个数
    if (checkedsub1 == chsub1) {
        $("#check_leagues").prop("checked", true);

    } else {
        $("#check_leagues").prop("checked", false);
    }
}

function updateFilterMatch() {
    var filterObj = $("input[name='leagues']");
    filterObj.prop("checked", false);
    var obj = $(".m-tab tr:visible");
    for (var i = 0; i < obj.length; i++) {
        var lIndex = obj.eq(i).attr("lindex");
        for (var j = 0; j < filterObj.length; j++) {

            if (filterObj.eq(j).attr("value") == lIndex) {
                if (filterObj.eq(j).prop("checked") != true) {
                    filterObj.eq(j).prop("checked", true);
                }

            }
        }
    }
}

function updateMathchNumbers() {
    var obj = $("table.m-tab tr:visible");
    var len = $("table.m-tab tr:visible").length;
    $(".u-org").html(len);
}

function updateTableCss() {
    //$("table.m-tab").each(function (index, element) {
    $("table.m-tab").find("tr:visible").each(function (i, element) {
        if (i % 2 == 0) {
            $(element).removeClass("odd");
            $(element).addClass("even");
            //console.log(i, "blue");
        } else {
            $(element).addClass("odd");
            $(element).removeClass("even");
            //console.log(i, "blue");
        }
    });
    //});
}
// 暂定赛程
function selectAllWeekLi() {
    if ($("#check_week_li").prop("checked")) {
        $("input[name='week_li']").prop("checked", true);
        $("#check_leagues_li").prop("checked", true);
        $("input[name='leagues_li']").prop("checked", true);
        $("ul.m-tab").show();
        $("ul.m-tab li").show();
        $(".bDateHideLi").html("隐藏");
    } else {
        $("input[name='week_li']").prop("checked", false);

        $("#check_leagues_li").prop("checked", false);
        $("input[name='leagues_li']").prop("checked", false);
        $("ul.m-tab li").hide();
        $(".bDateHideLi").html("显示");
    }
    updateMathchNumbersLi();
    updateLiCss();
}

$(document).on("click", "input[name='week_li']", function () {
    var tmpObj = $(this).closest("td");
    var windex = tmpObj.find("input").index($(this)) - 1;
    var tmpObj = $(".week" + windex);
    if ($(this).prop("checked")) {
        $(".week" + windex).show();
        $(".week" + windex + " li[windex=" + windex + "]").show();
        $(".week" + windex).prev().find("a").html("隐藏");
        $(".none").show();
    } else {
        $(".week" + windex + " li[windex=" + windex + "]").hide();
        $(".week" + windex).prev().find("a").html("显示");
        $(".none").hide();
    }

    updateFilterMatchLi();
    setSelectAllFilterLi();
    updateMathchNumbersLi();
    updateLiCss();
})

function selectAllLeaguesLi() {

    if ($("#check_leagues_li").prop("checked")) {
        $("input[name='leagues_li']").prop("checked", true);

        $("#check_week_li").prop("checked", true);
        $("input[name='week_li']").prop("checked", true);
        $("ul.m-tab").show();
        $("ul.m-tab li").show();
        $(".bDateHideLi").html("隐藏");
    } else {
        $("input[name='leagues_li']").prop("checked", false);

        $("#check_week_li").prop("checked", false);
        $("input[name='week_li']").prop("checked", false);
        $("ul.m-tab li").hide();
        $(".bDateHideLi").html("显示");
    }
    updateMathchNumbersLi();
    updateLiCss();
}

$(document).on("click", "input[name='leagues_li']", function () {
    var tmpObj = $(this).closest("td");
    //var index = tmpObj.find("input").index($(this)) - 1;
    var index = $(this).val();
    if ($(this).prop("checked")) {
        $("ul.m-tab li[lindex=" + index + "]").show();

        $("ul.m-tab").each(function (i, element) {
            var lindex = $(element).find("li[lindex=" + index + "]").attr("lindex");
            var windex = $(element).find("li[lindex=" + index + "]").attr("windex");
            if (lindex == index) {
                $("input[name='week_li']").eq(windex).prop("checked", true);
                $(".week" + windex).each(function (index, element) {
                    if ($(this).find("li:visible").length > 0) {
                        $(this).prev().find("a").html("隐藏");
                    }
                });
                return;
            }

        })
    } else {
        $("ul.m-tab li[lindex=" + index + "]").hide();
        $("ul.m-tab").each(function (i, element) {
            var len = $(element).find("li:visible").length;
            var windex = $(element).find("li[lindex=" + index + "]").attr("windex");
            if ($.trim($(element).find("li:visible").eq(len - 1).text() == '')) {
                len = len - 1;
            }
            if (len <= 0) {
                var w = $(element).find("li[lindex=" + index + "]").attr("windex");
                if (w != "undefined") {
                    $("input[name='week_li']").eq(w).prop("checked", false);
                    $(".week" + w).prev().find("a").html("显示");
                }

            }

            var is_have = false;
            $(".week" + windex).each(function (i, element) {
                if ($(this).find("li:visible").length > 0) {
                    $(this).prev().find("a").html("隐藏");
                    is_have = true;
                    return;

                } else {
                    $(this).prev().find("a").html("显示");
                }
            })

            if (is_have) {
                $("input[name='week_li']").eq(windex).prop("checked", true);

            } else {
                $("input[name='week_li']").eq(windex).prop("checked", false);

            }

        })
    }
    updateFilterMatchLi();
    setSelectAllFilterLi();
    updateMathchNumbersLi();
    updateLiCss();
})

$(document).on("click", ".bDateHideLi", function () {
    var weekindex = $(this).attr("weekindex");
    var obj = $(this).parent().next();
    if ($(this).html() == '显示') {
        obj.css("display", "block");
        obj.find("li").show();
        $(this).html("隐藏");
        $("input[name='week_li']").each(function (index) {
            if (index== weekindex) {
                $(this).prop("checked", true);
            }
        })
    } else {
        obj.css("display", "none");
        $(this).html("显示");
        obj.find("li").hide();
        $("input[name='week_li']").each(function (index) {
            if (index == weekindex) {
                $(this).prop("checked", false);
            }
        })
    }
    updateFilterMatchLi();
    setSelectAllFilterLi();
    updateMathchNumbersLi();
    updateLiCss();
})

function setSelectAllFilterLi() {
    var chsub = $("input[name='week_li']").length; //获取subcheck的个数
    var checkedsub = $("input[name='week_li']:checked").length; //获取选中的subcheck的个数
    if (checkedsub == chsub) {
        $("#check_week_li").prop("checked", true);

    } else {
        $("#check_week_li").prop("checked", false);
    }
    var chsub1 = $("input[name='leagues_li']").length; //获取subcheck的个数
    var checkedsub1 = $("input[name='leagues_li']:checked").length; //获取选中的subcheck的个数
    if (checkedsub1 == chsub1) {
        $("#check_leagues_li").prop("checked", true);

    } else {
        $("#check_leagues_li").prop("checked", false);
    }
}

function updateFilterMatchLi() {
    var filterObj = $("input[name='leagues_li']");
    filterObj.prop("checked", false);
    var obj = $("ul.m-tab li:visible");
    for (var i = 0; i < obj.length; i++) {
        var lIndex = obj.eq(i).attr("lindex");
        for (var j = 0; j < filterObj.length; j++) {
            if (filterObj.eq(j).attr("value") == lIndex) {
                if (filterObj.eq(j).prop("checked") != true) {
                    filterObj.eq(j).prop("checked", true);
                }

            }
        }
    }
}

function updateMathchNumbersLi() {
    var num = 0;
    $('.week').each(function (index, element) {
        var obj = $(this).next()
        var len = $(this).next().find("li:visible").length;
        var txt = $(this).next().find("li:visible").eq(len - 1).text();
        if ($.trim(txt) == "") {
            if (len > 1) {
                len = len - 1;
            }
        }
        num = num + len;

    });
    $(".u-org").html(num);

}

function getMatchesNums() {
    $('.week').each(function (index, element) {
        var obj = $(this).next()
        var len = $(this).next().find("li").length
        var txt = $(this).next().find("li").eq(len - 1).text();
        if ($.trim(txt) == "") {
            len = len - 1;
        }
        $(this).find("label").html(len);

    });
}
function updateLiCss() {
    $("ul.m-tab").each(function (index, element) {
        $(element).find("li:visible").each(function (i, e) {
            if (i % 2 == 1 ) {
                $(element).find("li:visible").eq(i).removeClass("odd");
                $(element).find("li:visible").eq(i).addClass("even");
            } else {
                $(element).find("li:visible").eq(i).removeClass("even");
                $(element).find("li:visible").eq(i).addClass("odd");

            }
        });

    });
}

// 登录调用，证明已经有值
function callListdata() {
    jcSzsc.scanChannel = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1
    jcSzsc.scanTokenTag = window.sessionStorage.getItem('getScanToken')|| ''
    jcSzsc.canLoginTag = true
    jcSzsc.loadSubscribedTeams();
}

