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
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "omar-wfs.fullname" -}}
{{-   if .Values.fullnameOverride }}
{{-     .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{-   else }}
{{-     $name := default .Chart.Name .Values.nameOverride }}
{{-     if contains $name .Release.Name }}
{{-       .Release.Name | trunc 63 | trimSuffix "-" }}
{{-     else }}
{{-       printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{-     end }}
{{-   end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "omar-wfs.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "omar-wfs.labels" -}}
omar-wfs.sh/chart: {{ include "omar-wfs.chart" . }}
{{ include "omar-wfs.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "omar-wfs.selectorLabels" -}}
app.kubernetes.io/name: {{ include "omar-wfs.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

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
    claimName: "{{ $.Values.fullnameOverride }}-{{ $volumeName }}-pvc"
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes" -}}
{{- include "omar-wfs.volumes.configmaps" . -}}
{{- include "omar-wfs.volumes.pvcs" . -}}
{{- end -}}
