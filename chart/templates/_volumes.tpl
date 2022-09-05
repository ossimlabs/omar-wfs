
{{/*
Templates for the volumeMounts section
*/}}

{{- define "omar-wfs.volumeMounts.configmaps" -}}
{{- range $configmapName, $configmapDict := .Values.configmaps}}
- name: {{ $configmapName | quote }}
  mountPath: {{ $configmapDict.mountPath | quote }}
  {{- if $configmapDict.subPath }}
  subPath: {{ $configmapDict.subPath | quote }}
  {{- end }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumeMounts.secrets" -}}
{{- range $secretName, $secretDict := .Values.secrets}}
- name: {{ $secretName | quote }}
  mountPath: {{ $secretDict.mountPath | quote }}
  {{- if $secretDict.subPath }}
  subPath: {{ $secretDict.subPath | quote }}
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
{{- include "omar-wfs.volumeMounts.secrets" . -}}
{{- include "omar-wfs.volumeMounts.pvcs" . -}}
{{- if .Values.global.extraVolumeMounts }}
{{ toYaml .Values.global.extraVolumeMounts }}
{{- end }}
{{- if .Values.extraVolumeMounts }}
{{ toYaml .Values.extraVolumeMounts }}
{{- end }}
{{- end -}}




{{/*
Templates for the volumes section
 */}}

{{- define "omar-wfs.volumes.configmaps" -}}
{{- range $configmapName, $configmapDict := .Values.configmaps}}
- name: {{ $configmapName | quote }}
  configMap:
    name: {{ $configmapName | quote }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes.secrets" -}}
{{- range $secretName, $secretDict := .Values.secrets}}
- name: {{ $secretName | quote }}
  secret:
    secretName: {{ $secretName | quote }}
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes.pvcs" -}}
{{- range $volumeName := .Values.volumeNames }}
{{- $volumeDict := index $.Values.global.volumes $volumeName }}
- name: {{ $volumeName }}
  persistentVolumeClaim:
    claimName: "{{ include "omar-wfs.fullname" $ }}-{{ $volumeName }}-pvc"
{{- end -}}
{{- end -}}

{{- define "omar-wfs.volumes" -}}
{{- include "omar-wfs.volumes.configmaps" . -}}
{{- include "omar-wfs.volumes.secrets" . -}}
{{- include "omar-wfs.volumes.pvcs" . -}}
{{- if .Values.global.extraVolumes }}
{{ toYaml .Values.global.extraVolumes }}
{{- end }}
{{- if .Values.extraVolumes }}
{{ toYaml .Values.extraVolumes }}
{{- end }}
{{- end -}}
