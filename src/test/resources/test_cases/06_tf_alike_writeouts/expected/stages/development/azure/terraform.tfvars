grafana_aad_app = {
  "grafana_dummy_dev" = {
    app_owners                   = ["Mr x","Mr y"]
    app_role_assignment_required = true
    display_name                 = "Grafana Dummy Development"
    logout_url                   = "hello.com/logout"
    redirect_uris                = ["hello-x.com","hello-y.com"]
    roles =  {"veng_developer" = {
    "app_role_id" = "12341234-1234-1234-1234-12341234"
    "principal_object_id" = "43214321-1234-1234-1234-43214321"
},
"admin" = {
    "app_role_id" = "12341234-1234-1234-1234-12341234"
    "principal_object_id" = "43214321-1234-1234-1234-43214321"
}}
  }
}

external_secrets_aad_app = {
  external_secrets_dummy_dev = {
    app_owners                   = ["Mr x","Mr y"]
    app_role_assignment_required = false
    display_name                 = "External Secrets Dummy Development"
    logout_url                   = "hello.com/logout"
    redirect_uris                = ["hello-x.com","hello-y.com"]
    roles                        = {}
  }
}