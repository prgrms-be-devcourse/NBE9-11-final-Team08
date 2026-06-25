output "vpc_id" {
  description = "ID of the project VPC."
  value       = aws_vpc.app.id
}

output "public_subnet_id" {
  description = "ID of the public subnet hosting the EC2 instances."
  value       = aws_subnet.public.id
}

output "edge_elastic_ip" {
  description = "Elastic IP to use for the external DNS A record."
  value       = aws_eip.edge.public_ip
}

output "elastic_ip" {
  description = "Alias of edge_elastic_ip for existing deployment notes."
  value       = aws_eip.edge.public_ip
}

output "app_public_ip" {
  description = "Public IP of the Spring application host, used for SSH deployment."
  value       = aws_instance.app.public_ip
}

output "app_private_ip" {
  description = "Private IP of the Spring application host. Use this as BACKEND_UPSTREAM with :8080 on the edge server."
  value       = aws_instance.app.private_ip
}

output "edge_ssh_command" {
  description = "SSH command for the Nginx edge host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_eip.edge.public_ip}"
}

output "app_ssh_command" {
  description = "SSH command for the Spring application host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_instance.app.public_ip}"
}

output "dns_instruction" {
  description = "External DNS record to create before running the HTTPS bootstrap."
  value       = "Create an A record for your API domain pointing to ${aws_eip.edge.public_ip}."
}
