use lazy_static::lazy_static;
use tailcall_valid::Validator;

use crate::cli::fmt::Fmt;
use crate::core::blueprint::{compile_service, Blueprint};
use crate::core::http::API_URL_PREFIX;
use crate::core::ir::model::IR;
use crate::core::print_schema;
use crate::core::rest::{EndpointSet, Unchecked};

pub const TAILCALL_RC: &str = ".tailcallrc.graphql";
pub const GRAPHQL_RC: &str = ".graphqlrc.yml";
pub const TAILCALL_RC_SCHEMA: &str = ".tailcallrc.schema.json";

lazy_static! {
    pub static ref TRACKER: tailcall_tracker::Tracker = tailcall_tracker::Tracker::default();
}

pub(super) fn log_endpoint_set(endpoint_set: &EndpointSet<Unchecked>) {
    let mut endpoints = endpoint_set.get_endpoints().clone();
    endpoints.sort_by(|a, b| {
        let method_a = a.get_method();
        let method_b = b.get_method();
        if method_a.eq(method_b) {
            a.get_path().as_str().cmp(b.get_path().as_str())
        } else {
            method_a.to_string().cmp(&method_b.to_string())
        }
    });
    for endpoint in endpoints {
        tracing::info!(
            "Endpoint: {} {}{} ... ok",
            endpoint.get_method(),
            API_URL_PREFIX,
            endpoint.get_path().as_str()
        );
    }
}

pub(super) fn display_schema(blueprint: &Blueprint) -> String {
    Fmt::display(Fmt::heading("GraphQL Schema:\n"));
    let schema = blueprint.to_schema();
    let sdl = print_schema::print_schema(schema);
    Fmt::display(format!("{}\n", sdl));
    sdl
}

pub(super) fn display_sdl(blueprint: &Blueprint) -> String {
    Fmt::display(Fmt::heading("GraphQL SDL:\n"));
    let schema = blueprint.to_schema();
    let mut sdl = print_schema::print_schema(schema);
    let ir = compile_service(sdl).to_result().unwrap();
    sdl = match ir {
        IR::Service(sdl) => sdl,
        _ => unreachable!(),
    };
    Fmt::display(format!("{}\n", sdl));
    sdl
}
