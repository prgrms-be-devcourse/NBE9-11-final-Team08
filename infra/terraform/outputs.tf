output "vpc_id" {
  description = "ID of the project VPC."
  value       = aws_vpc.app.id
}

output "public_subnet_id" {
  description = "ID of the public subnet hosting the EC2 instance."
  value       = aws_subnet.public.id
}

output "elastic_ip" {
  description = "Elastic IP to use for the external DNS A record."
  value       = aws_eip.app.public_ip
}

output "ssh_command" {
  description = "SSH command for the EC2 host."
  value       = "ssh -i <private-key-path> ubuntu@${aws_eip.app.public_ip}"
}

output "dns_instruction" {
  description = "External DNS record to create before running the HTTPS bootstrap."
  value       = "Create an A record for your API domain pointing to ${aws_eip.app.public_ip}."
}
