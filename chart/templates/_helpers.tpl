{{- define "omar-wfs.imagePullSecret" }}
{{- printf "{\"auths\": {\"%s\": {\"auth\": \"%s\"}}}" .Values.global.imagePullSecret.registry (printf "%s:%s" .Values.global.imagePullSecret.username .Values.global.imagePullSecret.password | b64enc) | b64enc }}
{{- end }}

{{/* Template for env vars */}}
{{- define "omar-wfs.envVars" -}}
  {{- range $key, $value := .Values.envVars }}
  - name: {{ $key | quote }}
    value: {{ $value | quote }}
  {{- end }}
{{- end -}}

{{/*
Return the proper image name
*/}}
{{- define "omar-wfs.image" -}}
{{- $registryName := .Values.image.registry -}}
{{- $imageName := .Values.image.name -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion | toString -}}
{{- if .Values.global }}
    {{- if .Values.global.dockerRepository }}
        {{- printf "%s/%s:%s" .Values.global.dockerRepository $imageName $tag -}}
    {{- else -}}
        {{- printf "%s/%s:%s" $registryName $imageName $tag -}}
    {{- end -}}
{{- else -}}
    {{- printf "%s/%s:%s" $registryName $imageName $tag -}}
{{- end -}}
{{- end -}}

{{/* Templates for the volumeMounts section */}}

{{- define "omar-wfs.volumeMounts.configmaps" -}}
{{- range $configmapName, $configmapDict := .Values.configmaps}}
- name: {{ $configmapName | quote }}
  mountPath: {{ $configmapDict.mountPath | quote }}
  {{- if $configmapDict.subPath }}
  subPath: {{ $configmapDict.subPath | quote }}
  {{- end }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumeMounts.pvcs" -}}
{{- range $volumeName := .Values.volumeNames }}
{{- $volumeDict := index $.Values.global.volumes $volumeName }}
- name: {{ $volumeName }}
  mountPath: {{ $volumeDict.mountPath }}
  {{- if $volumeDict.subPath }}
  subPath: {{ $volumeDict.subPath | quote }}
  {{- end }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumeMounts" -}}
{{- include "omar-wfs.volumeMounts.configmaps" . -}}
{{- include "omar-wfs.volumeMounts.pvcs" . -}}
{{- end -}}





{{/* Templates for the volumes section */}}

{{- define "omar-wfs.volumes.configmaps" -}}
{{- range $configmapName, $configmapDict := .Values.configmaps}}
- name: {{ $configmapName | quote }}
  configMap:
    name: {{ $configmapName | quote }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes.pvcs" -}}
{{- range $volumeName := .Values.volumeNames }}
{{- $volumeDict := index $.Values.global.volumes $volumeName }}
- name: {{ $volumeName }}
  persistentVolumeClaim:
    claimName: "{{ $.Values.appName }}-{{ $volumeName }}-pvc"
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes" -}}
{{- include "omar-wfs.volumes.configmaps" . -}}
{{- include "omar-wfs.volumes.pvcs" . -}}
{{- end -}}
