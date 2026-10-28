'use strict';
const $=id=>document.getElementById(id);
let token='',panel=null,draft=null,saving=false,uncertain=null,conflict=false,sessionEpoch=0,refreshing=false;
const words={tema:'Fondo',accesorio:'Accesorio',insignia:'Insignia'},symbols={estrella:'★',medalla:'◉',diamante:'◆',corazon:'♥',rayo:'ϟ',flor:'✿',tema:'◐',accesorio:'✧'};
function el(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
async function api(path,options={}){
 const abort=new AbortController(),timer=setTimeout(()=>abort.abort(),15000);
 try{const r=await fetch(path,{...options,headers:{'Content-Type':'application/json',...(token?{Authorization:`Bearer ${token}`}:{})},signal:abort.signal,cache:'no-store'});const data=await r.json().catch(()=>({}));if(!r.ok){const e=new Error(data.mensaje||'No pudimos completar la consulta.');e.status=r.status;throw e;}return data;
 }catch(e){if(e.name==='AbortError')throw new Error('La consulta tardó demasiado. Reintenta.');if(e instanceof TypeError)throw new Error('No se pudo conectar con NEXO. Comprueba que el servidor esté disponible.');throw e;}finally{clearTimeout(timer);}
}
function clearSession(message=''){sessionEpoch++;token='';panel=null;draft=null;uncertain=null;if($('editor').open)$('editor').close();if($('trash-dialog').open)$('trash-dialog').close();['metrics','featured','catalog','history','activity'].forEach(id=>$(id).replaceChildren());$('login-view').hidden=false;$('panel-view').hidden=true;$('login-error').textContent=message;$('login-form').elements.password.value='';$('notice').textContent='';$('panel-error').textContent='';$('search').value='';$('filter').value='todos';$('type-filter').value='todos';$('sort').value='nombre';$('history-search').value='';$('data-status').textContent='Sin consulta reciente';}
async function revoke(){const pending=token?api('/bienestar/salir',{method:'POST'}):Promise.resolve();clearSession();try{await pending;}catch{}}
function showPage(name){['resumen','premios','historial'].forEach(p=>$(p+'-page').hidden=p!==name);document.querySelectorAll('nav [data-page]').forEach(b=>{b.classList.toggle('active',b.dataset.page===name);if(b.dataset.page===name)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current');});$('page-title').textContent={resumen:'Vista general',premios:'Premios',historial:'Historial'}[name];$('page-title').focus();}
document.querySelectorAll('[data-page]').forEach(b=>b.addEventListener('click',()=>showPage(b.dataset.page)));
$('login-form').addEventListener('submit',async e=>{e.preventDefault();const b=e.submitter;b.disabled=true;$('login-error').textContent='';try{const form=e.target;const res=await api('/login',{method:'POST',body:JSON.stringify({correo:form.elements.correo.value.trim(),password:form.elements.password.value})});form.elements.password.value='';token=res.token;if(!token)throw new Error('El servidor no devolvió una sesión válida.');panel=await api('/admin/api/panel');render();$('login-view').hidden=true;$('panel-view').hidden=false;showPage('resumen');$('page-title').tabIndex=-1;$('page-title').focus();}catch(err){await revoke();$('login-error').textContent=err.message;}finally{b.disabled=false;}});
$('logout').addEventListener('click',revoke);
async function refresh(){
 if(refreshing)return;
 const epoch=sessionEpoch,b=$('refresh');refreshing=true;b.disabled=true;b.textContent='Actualizando…';$('panel-error').textContent='';$('data-status').textContent='Consultando datos…';
 try{const result=await api('/admin/api/panel');if(epoch!==sessionEpoch)return;panel=result;render();}
 catch(e){if(epoch!==sessionEpoch)return;if(e.status===401||e.status===403)clearSession(e.message);else{$('panel-error').textContent=e.message;$('data-status').textContent='Sin actualizar · se conserva la última consulta';$('data-status').classList.add('stale');}}
 finally{refreshing=false;b.disabled=false;b.textContent='↻ Actualizar';}
}
$('refresh').addEventListener('click',refresh);
function art(p){return el('div',symbols[p.recurso]||symbols[p.tipo]||'★','badge-art '+p.tipo+' palette-'+(p.color||'ambar')+' background-'+(p.fondo||'suave')+' theme-'+p.recurso);}
function render(){
 $('activity').replaceChildren();const actividad=panel.actividad||[];const maximo=Math.max(1,...actividad.map(d=>d.revisiones));
 actividad.forEach(d=>{const c=el('div',undefined,'activity-day'),f=new Date(d.fecha+'T12:00:00'),time=el('time',f.toLocaleDateString('es-MX',{day:'numeric',month:'short'}));time.dateTime=d.fecha;const m=el('meter');m.min=0;m.max=maximo;m.value=d.revisiones;m.setAttribute('aria-label',d.revisiones+' revisiones el '+d.fecha);c.append(el('span',f.toLocaleDateString('es-MX',{weekday:'short'})),el('strong',String(d.revisiones)),m,time);$('activity').append(c);});
 if(!actividad.length)$('activity').append(el('p','Aún no hay datos de actividad disponibles.','empty'));
 const r=panel.resumen;$('metrics').replaceChildren();[[r.usuarios,'Cuentas registradas','Comunidad'],[r.revisionesHoy,'Revisiones de hoy','Horario de Monterrey'],[r.metasActivas,'Metas activas','Objetivos en marcha'],[r.premiosActivos,'Premios publicados','Disponibles en la app']].forEach(([n,l,s])=>{const c=el('div',undefined,'metric');c.append(el('span',l),el('strong',String(n)),el('small',s));$('metrics').append(c);});$('featured').replaceChildren();panel.premios.filter(p=>p.activo&&!p.eliminado).slice(0,3).forEach(p=>{const row=el('div',undefined,'feature'),t=el('div');t.append(el('strong',p.nombre),el('p',`${words[p.tipo]} · ${daysLabel(p.dias)}`));row.append(art(p),t);$('featured').append(row);});renderCatalog();renderHistory();$('data-status').classList.remove('stale');$('data-status').textContent='Datos consultados · '+new Date().toLocaleTimeString('es-MX',{hour:'2-digit',minute:'2-digit'});$('updated').textContent='Actualizado '+new Date().toLocaleTimeString('es-MX',{hour:'2-digit',minute:'2-digit'});}
function normalize(text){return String(text).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('es').trim();}
function renderCatalog(){
 if(!panel)return;
 const q=normalize($('search').value),f=$('filter').value,t=$('type-filter').value,sort=$('sort').value;
 const premios=panel.premios.filter(p=>normalize(p.nombre+' '+p.descripcion).includes(q)&&(f==='papelera'?p.eliminado:!p.eliminado&&(f==='todos'||(f==='activos'?p.activo:!p.activo)))&&(t==='todos'||p.tipo===t))
 .sort((a,b)=>(sort==='dias'?a.dias-b.dias:sort==='dias-desc'?b.dias-a.dias:0)||a.nombre.localeCompare(b.nombre,'es'));
 $('catalog').replaceChildren();$('count').textContent=`${premios.length} de ${panel.premios.length} premios`;
 premios.forEach(p=>{const c=el('article',undefined,'prize'),top=el('div',undefined,'top');top.append(art(p),el('span',p.eliminado?'En papelera':p.activo?'Publicado':'Pausado','pill'+(p.activo?'':' paused')));c.append(top,el('small',words[p.tipo],'muted'),el('h3',p.nombre),el('p',p.descripcion),el('div',daysLabel(p.dias),'requirement'));const b=el('button','Editar premio','secondary');b.setAttribute('aria-label','Editar premio '+p.nombre);b.addEventListener('click',()=>openEditor(p));if(!p.eliminado)c.append(b);if(p.dias>0){const quitar=el('button',p.eliminado?'Restaurar pausado':'Eliminar','text-button');quitar.setAttribute('aria-label',(p.eliminado?'Restaurar premio ':'Eliminar premio ')+p.nombre);quitar.addEventListener('click',()=>openTrash(p));c.append(quitar);}$('catalog').append(c);});
 if(!premios.length)$('catalog').append(el('p','No hay coincidencias. Cambia la búsqueda o limpia los filtros.','empty'));
}
function daysLabel(days){return days===0?'Disponible desde el inicio':days===1?'1 día revisado':`${days} días revisados`;}
function renderHistory(){
 if(!panel)return;
 const q=normalize($('history-search').value);
 const acciones={CREAR:'Creación',EDITAR:'Edición',ELIMINAR:'Enviado a papelera',RESTAURAR:'Restauración'};
 const rows=panel.historial.map(h=>[acciones[h.accion]||h.accion,panel.premios.find(p=>p.id===h.premio)?.nombre||h.premio,'Cuenta #'+h.administrador,new Date(h.fecha).toLocaleString('es-MX')]).filter(row=>normalize(row.join(' ')).includes(q));
 $('history').replaceChildren();$('history-count').textContent=`${rows.length} de ${panel.historial.length} acciones recientes`;
 rows.forEach(row=>{const tr=el('tr');row.forEach(t=>tr.append(el('td',t)));$('history').append(tr);});
 if(!rows.length){const tr=el('tr'),td=el('td',q?'No hay acciones que coincidan con la búsqueda.':'Todavía no hay cambios administrativos.','empty');td.colSpan=4;tr.append(td);$('history').append(tr);}
}
function previewReward(){
 if(!draft)return;
 const p={...draft,nombre:$('reward-name').value.trim()||'Nombre de tu premio',descripcion:$('reward-description').value.trim()||'Aquí aparecerá lo que celebra este premio.',recurso:$('reward-art').value,dias:Number($('reward-days').value),tipo:$('reward-type').value,color:$('reward-color').value,fondo:$('reward-background').value};
 const row=el('div',undefined,'feature'),text=el('div');text.append(el('strong',p.nombre),el('p',p.descripcion),el('p',Number.isInteger(p.dias)&&p.dias>=0?daysLabel(p.dias):'Indica los días requeridos'));row.append(art(p),text);$('reward-preview').replaceChildren(row);
 $('publication-summary').textContent=$('reward-active').checked?'Publicado: aparecerá en el catálogo al actualizar la app.':'Pausado: se conserva para quienes ya lo ganaron; no se ofrece a nuevos usuarios.';
}
$('search').addEventListener('input',renderCatalog);
['filter','type-filter','sort'].forEach(id=>$(id).addEventListener('change',renderCatalog));
$('reset-filters').addEventListener('click',()=>{$('search').value='';$('filter').value='todos';$('type-filter').value='todos';$('sort').value='nombre';renderCatalog();});
$('history-search').addEventListener('input',renderHistory);
['reward-name','reward-description','reward-art','reward-days','reward-active','reward-color','reward-background'].forEach(id=>$(id).addEventListener('input',previewReward));
function openEditor(p=null){uncertain=null;conflict=false;draft=p?{...p}:{id:'insignia-'+crypto.randomUUID(),nombre:'',descripcion:'',tipo:'insignia',recurso:'estrella',dias:14,activo:true,version:0,color:'ambar',fondo:'suave',eliminado:false};$('reward-type').value=draft.tipo;$('reward-color').value=draft.color||'ambar';$('reward-background').value=draft.fondo||'suave';$('badge-options').hidden=draft.tipo!=='insignia';$('editor-title').textContent=p?'Editar premio':'Nuevo premio';$('reward-name').value=draft.nombre;$('reward-description').value=draft.descripcion;$('reward-days').value=draft.dias;const select=$('reward-art');select.replaceChildren();(p?[[p.recurso,p.tipo==='insignia'?p.recurso:words[p.tipo]+' · '+p.recurso]]:[['estrella','★ Estrella'],['medalla','◉ Medalla'],['diamante','◆ Diamante'],['corazon','♥ Corazón'],['rayo','ϟ Rayo'],['flor','✿ Flor']]).forEach(([v,t])=>{const o=el('option',t);o.value=v;select.append(o);});select.value=draft.recurso;$('reward-active').checked=draft.activo;$('reward-days').min=p&&p.dias===0?'0':'1';$('save-error').textContent='';$('save').textContent='Guardar premio';lockForm(false);previewReward();$('editor').showModal();$('reward-name').focus();}
function lockForm(locked){['reward-name','reward-description'].forEach(id=>$(id).disabled=locked);['reward-type','reward-color','reward-background'].forEach(id=>$(id).disabled=locked||draft.version>0);$('reward-art').disabled=locked||draft.version>0;$('reward-days').disabled=locked||draft.version>0;$('reward-active').disabled=locked||draft.dias===0;$('save').disabled=saving||conflict;$('close-editor').disabled=saving;$('cancel-editor').disabled=saving;}
function closeEditor(){if(!saving){$('editor').close();draft=null;uncertain=null;}}
$('create').addEventListener('click',()=>openEditor());$('hero-create').addEventListener('click',()=>openEditor());$('close-editor').addEventListener('click',closeEditor);$('cancel-editor').addEventListener('click',closeEditor);$('editor').addEventListener('cancel',e=>{if(saving)e.preventDefault();});
$('reward-form').addEventListener('submit',async e=>{e.preventDefault();if(saving||conflict)return;const epoch=sessionEpoch;const body=uncertain||{...draft,nombre:$('reward-name').value.trim(),descripcion:$('reward-description').value.trim(),recurso:$('reward-art').value,dias:Number($('reward-days').value),activo:$('reward-active').checked,tipo:$('reward-type').value,color:$('reward-color').value,fondo:$('reward-background').value};saving=true;lockForm(true);$('save-error').textContent='';$('save').textContent='Guardando…';try{await api('/admin/api/premios/'+encodeURIComponent(body.id),{method:'PUT',body:JSON.stringify(body)});if(epoch!==sessionEpoch)return;saving=false;closeEditor();$('notice').textContent='Premio guardado. Aparecerá al actualizar Perfil en la app.';await refresh();}catch(err){if(epoch!==sessionEpoch)return;if(err.status===401||err.status===403)clearSession(err.message);else if(err.status===409){conflict=true;$('save-error').textContent=err.message+' Cierra el formulario y actualiza el catálogo.';}else{uncertain=err.status===400?null:body;$('save-error').textContent=err.message+(uncertain?' Reintenta el mismo guardado para confirmar su resultado.':'');}}finally{saving=false;if(draft){lockForm(Boolean(uncertain)||conflict);$('save').textContent=uncertain?'Reintentar guardado':'Guardar premio';}}});

$('reward-type').addEventListener('change',()=>{
 if(!draft||draft.version>0)return;
 const tipo=$('reward-type').value;draft.id=(tipo==='tema'?'tema-':'insignia-')+crypto.randomUUID();draft.tipo=tipo;
 const choices=tipo==='tema'?[['noche','Noche'],['ciruela','Ciruela'],['cafe','Café']]:[['estrella','★ Estrella'],['medalla','◉ Medalla'],['diamante','◆ Diamante'],['corazon','♥ Corazón'],['rayo','ϟ Rayo'],['flor','✿ Flor']];
 $('reward-art').replaceChildren();choices.forEach(([value,label])=>{const option=el('option',label);option.value=value;$('reward-art').append(option);});
 $('badge-options').hidden=tipo!=='insignia';$('reward-color').value='ambar';$('reward-background').value='suave';previewReward();
});
let trashDraft=null,trashBusy=false;
function openTrash(p){$('trash-confirm').disabled=false;trashDraft={...p,eliminado:!p.eliminado,activo:false};$('trash-title').textContent=p.eliminado?'Restaurar premio':'Eliminar premio';$('trash-description').textContent=p.nombre+(p.eliminado?' regresará al catálogo pausado.':' dejará de ofrecerse y se moverá a la papelera.');$('trash-error').textContent='';$('trash-confirm').textContent=p.eliminado?'Restaurar pausado':'Mover a papelera';$('trash-dialog').showModal();}
$('trash-cancel').addEventListener('click',()=>{if(!trashBusy)$('trash-dialog').close();});
$('trash-dialog').addEventListener('cancel',e=>{if(trashBusy)e.preventDefault();});
$('trash-confirm').addEventListener('click',async()=>{
 if(trashBusy||!trashDraft)return;const epoch=sessionEpoch,body={...trashDraft};trashBusy=true;$('trash-confirm').disabled=true;$('trash-cancel').disabled=true;
 try{await api('/admin/api/premios/'+encodeURIComponent(body.id),{method:'PUT',body:JSON.stringify(body)});if(epoch!==sessionEpoch)return;$('trash-dialog').close();$('notice').textContent=body.eliminado?'Premio movido a la papelera. Los ya ganados se conservan.':'Premio restaurado pausado. Edítalo para publicarlo.';await refresh();}
 catch(e){if(epoch!==sessionEpoch)return;if(e.status===401||e.status===403){$('trash-dialog').close();clearSession(e.message);}else{$('trash-error').textContent=e.message+(e.status===409?' Cierra y actualiza el catálogo.':' Puedes reintentar la misma operación.');if(e.status===409)trashDraft=null;}}
 finally{trashBusy=false;$('trash-confirm').disabled=!trashDraft;$('trash-cancel').disabled=false;}
});
