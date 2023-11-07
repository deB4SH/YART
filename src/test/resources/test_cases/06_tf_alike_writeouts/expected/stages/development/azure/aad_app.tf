
module "grafana_aad_app" {
  source = "github.com/Hamburg-Port-Authority/terraform-azure-aad-app?ref=1.0.1"

  for_each                     = var.grafana_aad_app
  app_roles                    = var.grafana_app_roles
  display_name                 = each.value.display_name
  redirect_uris                = each.value.redirect_uris
  logout_url                   = each.value.logout_url
  app_owners                   = each.value.app_owners
  roles                        = each.value.roles
  app_role_assignment_required = each.value.app_role_assignment_required

}


module "external_secrets_aad_app" {
  source = "github.com/Hamburg-Port-Authority/terraform-azure-aad-app?ref=1.0.1"

  for_each                     = var.external_secrets_aad_app
  app_roles                    = {}
  display_name                 = each.value.display_name
  redirect_uris                = each.value.redirect_uris
  logout_url                   = each.value.logout_url
  app_owners                   = each.value.app_owners
  roles                        = each.value.roles
  app_role_assignment_required = each.value.app_role_assignment_required


}


