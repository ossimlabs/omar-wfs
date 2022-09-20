
{{/*
Constructs busybox image name.
*/}}
{{- define "omar-wfs.busybox.image" -}}
{{- $image := default .Values.global.busybox.image.repository .Values.busybox.image.repository }}
{{- $tag := default .Values.global.busybox.image.tag .Values.busybox.image.tag }}
{{- printf "%s:%s" $image ($tag | toString) -}}
{{- end -}}
