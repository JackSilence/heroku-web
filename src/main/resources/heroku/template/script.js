$('%1$s').css('width','432px').find('> thead > th.pull-right').text(''),$('a.ember-view','%1$s').each(function(){var t=$(this).text().trim(),e=t.split('-'),h=e.length;h>1&&$(this).html($(this).html().replace(t,'*'.repeat(e[0].length)+'-'+e.slice(1,h).join('-')))});